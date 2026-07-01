package com.health.report.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.report.common.core.domain.entity.SysDictData;
import com.health.report.domain.*;
import com.health.report.mapper.*;
import com.health.report.system.mapper.SysDictDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PythonScriptClient {

    /**
     * 从配置文件读取，生产环境禁止硬编码
     */
    @Value("${python.path:python}")
    private String pythonPath;
    private static final String pythonBaseDir = System.getProperty("user.dir") + "/";


    /**
     * 模型全局超时时间（生产核心：防止Python卡死）
     */
    private static final long PYTHON_TIMEOUT_SECONDS = 200L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JsonRecordMapper jsonRecordMapper;
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private MemberInfoMapper memberInfoMapper;
    @Autowired
    private ReportModelResultMapper reportModelResultMapper;
    @Autowired
    private SysDictDataMapper sysDictDataMapper;
    @Autowired
    private ReportModelIndicatorMapper reportModelIndicatorMapper;
    @Autowired
    private AiModelMapper aiModelMapper;
    @Autowired
    private ReportDiseaseRiskMapper reportDiseaseRiskMapper;

    public JSONObject callModel(
            Long reportId,
            Long modelId,
            Map<String, Object> params,
            String pyPath,
            String modelName,
            Integer priority,
            String category
    ) {
        Process process = null;
        try {
            JSONObject inputJson = new JSONObject();
            inputJson.put("report_id", reportId);
            inputJson.put("model_id", modelId);
            if (params != null) {
                inputJson.putAll(params);
            }

            String jsonParam = JSON.toJSONString(inputJson, JSONWriter.Feature.WriteNulls);
            String base64Param = Base64.getEncoder().encodeToString(jsonParam.getBytes(StandardCharsets.UTF_8));
            log.info("[模型调用-入参] reportId:{}, modelId:{}, param:{}", reportId, modelId, jsonParam);

            String fullScriptPath = pythonBaseDir + pyPath;
            String[] cmd = {pythonPath, fullScriptPath, base64Param};
            process = Runtime.getRuntime().exec(cmd);

            // 生产核心：超时熔断
            boolean finished = process.waitFor(PYTHON_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                log.error("[模型调用-超时] reportId:{}, modelId:{}, 超过{}秒", reportId, modelId, PYTHON_TIMEOUT_SECONDS);
                throw new RuntimeException("模型执行超时");
            }

            // 资源自动关闭
            String outputStr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                outputStr = reader.lines().collect(Collectors.joining()).trim();
            }

            String errorStr;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                errorStr = errorReader.lines().collect(Collectors.joining()).trim();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("[模型调用-失败] reportId:{}, modelId:{}, 退出码:{}, 错误:{}", reportId, modelId, exitCode, errorStr);
                throw new RuntimeException("模型执行失败，错误：" + errorStr);
            }

            if (outputStr.isEmpty()) {
                log.error("[模型调用-无输出] reportId:{}, modelId:{}", reportId, modelId);
                throw new RuntimeException("模型执行失败：无返回结果");
            }

            log.info("[模型调用-成功] reportId:{}, modelId:{}, 输出:{}", reportId, modelId, outputStr);
            JSONObject resultJson = JSONObject.parseObject(outputStr);
            saveJsonRecord(reportId, modelId, modelName, jsonParam, outputStr, priority, category);
            return resultJson;

        } catch (Exception e) {
            log.error("[模型调用-异常] reportId:{}, modelId:{}", reportId, modelId, e);
            throw new RuntimeException("模型调用异常：" + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void saveJsonRecord(Long reportId, Long modelId, String modelName, String inputJson, String outJson, Integer priority, String category) {
        try {
            JsonRecord in = new JsonRecord();
            in.setReportId(reportId);
            in.setModelId(modelId);
            in.setJsonContent(inputJson);
            in.setDataType(1);
            in.setRemark(modelName + "入参");
            in.setDeleted(0);
            in.setCreateTime(new Date());
            in.setUpdateTime(new Date());
            jsonRecordMapper.insertJsonRecord(in);

            JsonRecord out = new JsonRecord();
            out.setReportId(reportId);
            out.setModelId(modelId);
            out.setJsonContent(outJson);
            out.setDataType(2);
            out.setRemark(modelName + "结果");
            out.setDeleted(0);
            out.setCreateTime(new Date());
            out.setUpdateTime(new Date());
            jsonRecordMapper.insertJsonRecord(out);

            Report report = reportMapper.selectReportById(reportId);
            if (report != null) {
                MemberInfo memberInfo = memberInfoMapper.selectById(report.getMemberId());
                BigDecimal actualAge = new BigDecimal(calculateAge(memberInfo));
                BigDecimal biologyAge = parseBiologyAge(outJson);
                saveModelResult(reportId, modelId, modelName, priority, category, actualAge, biologyAge, outJson);
            }

            if ("disease".equals(category)) {
                AiModel aiModel = aiModelMapper.selectById(modelId);
                if (aiModel == null) return;

                List<SysDictData> dictDataList = sysDictDataMapper.selectDictDataByType("disease");
                BigDecimal riskProb = parseDiseaseRiskProbability(outJson);
                for (SysDictData dict : dictDataList) {
                    if (aiModel.getModelName().contains(dict.getDictLabel())) {
                        ReportDiseaseRisk risk = new ReportDiseaseRisk();
                        risk.setReportId(reportId);
                        risk.setModelId(modelId);
                        risk.setModelName(aiModel.getModelName());
                        risk.setDiseaseName(dict.getDictLabel());
                        risk.setSortOrder(dict.getDictSort().intValue());
                        risk.setRawResult(outJson);
                        risk.setRiskProbability(riskProb);
                        risk.setCreateTime(new Date());
                        risk.setUpdateTime(new Date());
                        reportDiseaseRiskMapper.insert(risk);
                    }
                }
            }

        } catch (Exception e) {
            log.error("[保存记录异常] reportId:{}, modelId:{}", reportId, modelId, e);
        }
    }

    private BigDecimal parseDiseaseRiskProbability(String jsonResult) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(jsonResult);
            if (root.has("十年风险概率")) {
                return BigDecimal.valueOf(root.get("十年风险概率").asDouble());
            }
        } catch (Exception e) {
            log.warn("解析疾病风险失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal parseBiologyAge(String outJson) {
        try {
            JSONObject json = JSONObject.parseObject(outJson);
            if (json.containsKey("生物学年龄")) return json.getBigDecimal("生物学年龄");
            if (json.containsKey("biology_age")) return json.getBigDecimal("biology_age");
            if (json.containsKey("biological_age")) return json.getBigDecimal("biological_age");
            if (json.containsKey("age")) return json.getBigDecimal("age");
        } catch (Exception e) {
            log.warn("解析生物年龄失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private void saveModelResult(Long reportId, Long modelId, String modelName, Integer modelPriority,
                                 String category, BigDecimal actualAge, BigDecimal biologyAge, String rawResult) {
        try {
            Integer dictPriority = getPriorityFromDict(category);
            Long resultId = doSaveModelResult(reportId, modelId, modelName, modelPriority, dictPriority, category, actualAge, biologyAge, rawResult);
            parseAndSaveIndicators(reportId, resultId, modelId, rawResult, modelPriority);
        } catch (Exception e) {
            log.error("[保存模型结果异常] reportId:{}", reportId, e);
        }
    }

    private Integer getPriorityFromDict(String category) {
        try {
            List<SysDictData> list = sysDictDataMapper.selectDictDataByType("model_category");
            if (list == null || list.isEmpty()) return 99;
            for (SysDictData d : list) {
                if (category.equals(d.getDictValue()) || category.equals(d.getDictLabel())) {
                    return d.getDictSort().intValue();
                }
            }
        } catch (Exception e) {
            log.error("获取优先级异常: {}", e.getMessage());
        }
        return 99;
    }

    public void cleanupWholeModelResults(Long reportId) {
        try {
            List<ReportModelResult> all = reportModelResultMapper.selectByReportId(reportId);
            List<ReportModelResult> wholes = all.stream()
                    .filter(r -> "whole".equals(r.getCategory()) || "整体情况".equals(r.getCategory()))
                    .toList();

            if (wholes.size() <= 1) return;

            Optional<ReportModelResult> highest = wholes.stream().min(Comparator.comparing(
                    r -> r.getModelPriority() == null ? 999 : r.getModelPriority()
            ));

            if (highest.isPresent()) {
                ReportModelResult keep = highest.get();
                for (ReportModelResult item : wholes) {
                    if (!item.getId().equals(keep.getId())) {
                        reportModelResultMapper.deleteById(item.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[清理整体模型异常] reportId:{}", reportId, e);
        }
    }

    private Long doSaveModelResult(Long reportId, Long modelId, String modelName, Integer modelPriority, Integer priority,
                                   String category, BigDecimal actualAge, BigDecimal biologyAge, String rawResult) {
        ReportModelResult r = new ReportModelResult();
        r.setReportId(reportId);
        r.setModelId(modelId);
        r.setModelName(modelName);
        r.setModelCode(modelName);
        r.setCategory(category);
        r.setModelPriority(modelPriority);
        r.setPriority(priority == null ? 99 : priority);
        r.setActualAge(actualAge.intValue());
        r.setBiologyAge(biologyAge);
        r.setAgeGap(biologyAge.subtract(actualAge));
        r.setRawResult(rawResult);
        r.setStatus(biologyAge.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
        r.setErrorMsg(biologyAge.compareTo(BigDecimal.ZERO) > 0 ? null : "未获取生物学年龄");
        r.setCreateTime(new Date());
        r.setUpdateTime(new Date());
        reportModelResultMapper.insert(r);
        return r.getId();
    }

    private void parseAndSaveIndicators(Long reportId, Long modelResultId, Long modelId, String rawResult, Integer modelPriority) {
        try {
            JSONObject json = JSONObject.parseObject(rawResult);
            if (json == null) return;

            int sort = 0;
            for (String key : json.keySet()) {
                if (key.endsWith("_贡献")) {
                    String name = key.substring(0, key.length() - 3);
                    BigDecimal contribution = json.getBigDecimal(key);
                    BigDecimal current = json.getBigDecimal(name);

                    ReportModelIndicator indicator = new ReportModelIndicator();
                    indicator.setReportId(reportId);
                    indicator.setModelResultId(modelResultId);
                    indicator.setModelPriority(modelPriority);
                    indicator.setModelId(modelId);
                    indicator.setIndicatorName(name);
                    indicator.setIndicatorCode(name);
                    indicator.setCurrentValue(current);
                    indicator.setCurrentValueStr(current != null ? current.toString() : null);
                    indicator.setAgeContribution(contribution);
                    indicator.setSortOrder(sort++);
                    indicator.setCreateTime(new Date());
                    indicator.setUpdateTime(new Date());
                    reportModelIndicatorMapper.insert(indicator);
                }
            }
        } catch (Exception e) {
            log.error("[解析指标异常] reportId:{}", reportId, e);
        }
    }

    public Integer calculateAge(MemberInfo member) {
        try {
            if (member == null || member.getBirthDate() == null) return 45;
            LocalDate birth = member.getBirthDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            return LocalDate.now().getYear() - birth.getYear();
        } catch (Exception e) {
            return 45;
        }
    }

    public static class SystemTypeInfo {
        private final String key;
        private final String label;
        private final Integer sort;

        public SystemTypeInfo(String key, String label, Integer sort) {
            this.key = key;
            this.label = label;
            this.sort = sort;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public Integer getSort() { return sort; }
    }

    private List<SystemTypeInfo> getSystemTypesFromDict() {
        try {
            List<SysDictData> list = sysDictDataMapper.selectDictDataByType("model_category");
            if (list != null && !list.isEmpty()) {
                return list.stream()
                        .filter(d -> !"disease".equals(d.getDictValue()))
                        .sorted(Comparator.comparing(SysDictData::getDictSort))
                        .map(d -> new SystemTypeInfo(d.getDictValue(), d.getDictLabel(), d.getDictSort().intValue()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("获取系统类型异常", e);
        }

        List<SystemTypeInfo> defaults = new ArrayList<>();
        defaults.add(new SystemTypeInfo("whole", "整体情况", 1));
        defaults.add(new SystemTypeInfo("cardiovascular", "心血管系统", 2));
        defaults.add(new SystemTypeInfo("kidney", "肾脏", 3));
        defaults.add(new SystemTypeInfo("endocrine", "内分泌系统", 4));
        defaults.add(new SystemTypeInfo("lung", "呼吸系统", 5));
        defaults.add(new SystemTypeInfo("digestive", "消化系统", 6));
        defaults.add(new SystemTypeInfo("immune", "免疫系统", 7));
        defaults.add(new SystemTypeInfo("blood", "造血系统", 8));
        defaults.add(new SystemTypeInfo("bone", "骨骼肌肉系统", 9));
        defaults.add(new SystemTypeInfo("nutrition", "营养评估", 10));
        return defaults;
    }

    public void fillMissingSystemTypes(Long reportId, BigDecimal actualAge) {
        try {
            List<SystemTypeInfo> types = getSystemTypesFromDict();
            List<ReportModelResult> exists = reportModelResultMapper.selectByReportId(reportId);

            for (SystemTypeInfo type : types) {
                boolean has = exists.stream().anyMatch(r ->
                        type.getKey().equals(r.getCategory()) || type.getLabel().equals(r.getCategory())
                );

                if (!has) {
                    ReportModelResult fill = new ReportModelResult();
                    fill.setReportId(reportId);
                    fill.setModelId(0L);
                    fill.setModelName("未调用模型-" + type.getLabel());
                    fill.setModelCode("none");
                    fill.setCategory(type.getKey());
                    fill.setModelPriority(999);
                    fill.setPriority(type.getSort());
                    fill.setActualAge(actualAge.intValue());
                    fill.setBiologyAge(actualAge);
                    fill.setAgeGap(BigDecimal.ZERO);
                    fill.setRawResult("{}");
                    fill.setStatus(0);
                    fill.setErrorMsg("未调用模型");
                    fill.setCreateTime(new Date());
                    fill.setUpdateTime(new Date());
                    reportModelResultMapper.insert(fill);
                }
            }
        } catch (Exception e) {
            log.error("[填充系统类型异常] reportId:{}", reportId, e);
        }
    }

    private String getCategoryKey(String category) {
        if (category == null) return null;
        return switch (category) {
            case "整体情况" -> "whole";
            case "心血管系统" -> "cardiovascular";
            case "肾脏" -> "kidney";
            case "内分泌系统" -> "endocrine";
            case "呼吸系统" -> "lung";
            case "消化系统" -> "digestive";
            case "免疫系统" -> "immune";
            case "造血系统" -> "blood";
            case "骨骼肌肉系统" -> "bone";
            case "营养评估" -> "nutrition";
            default -> category;
        };
    }

    public List<String> getSystemTypeLabels() {
        return getSystemTypeLabels("zh");
    }

    public List<String> getSystemTypeLabels(String language) {
        boolean en = "en".equals(language);
        return getSystemTypesFromDict().stream()
                .map(info -> en ? getSystemTypeLabelEn(info.getLabel()) : info.getLabel())
                .collect(Collectors.toList());
    }

    private String getSystemTypeLabelEn(String label) {
        if (label == null) return "";
        return switch (label) {
            case "整体情况" -> "Overall";
            case "心血管系统" -> "Cardiovascular";
            case "营养评估" -> "Nutrition";
            case "消化系统" -> "Digestive";
            case "呼吸系统" -> "Lung";
            case "造血系统" -> "Blood";
            case "免疫系统" -> "Immune";
            case "肾脏" -> "Kidney";
            case "内分泌系统" -> "Endocrine";
            case "骨骼肌肉系统" -> "Bone";
            default -> label;
        };
    }

    public void fillMissingDiseaseTypes(Long reportId) {
        try {
            List<SysDictData> dictList = sysDictDataMapper.selectDictDataByType("disease");
            if (dictList == null || dictList.isEmpty()) return;

            dictList.sort(Comparator.comparing(SysDictData::getDictSort));
            List<ReportDiseaseRisk> exists = reportDiseaseRiskMapper.selectByReportId(reportId);

            for (SysDictData dict : dictList) {
                boolean has = exists.stream().anyMatch(r -> dict.getDictLabel().equals(r.getDiseaseName()));
                if (!has) {
                    ReportDiseaseRisk risk = new ReportDiseaseRisk();
                    risk.setReportId(reportId);
                    risk.setModelId(0L);
                    risk.setModelName("未调用模型-" + dict.getDictLabel());
                    risk.setDiseaseName(dict.getDictLabel());
                    risk.setRiskProbability(BigDecimal.ZERO);
                    risk.setSortOrder(dict.getDictSort().intValue());
                    risk.setRawResult("{}");
                    risk.setCreateTime(new Date());
                    risk.setUpdateTime(new Date());
                    reportDiseaseRiskMapper.insert(risk);
                }
            }
        } catch (Exception e) {
            log.error("[填充疾病类型异常] reportId:{}", reportId, e);
        }
    }
}