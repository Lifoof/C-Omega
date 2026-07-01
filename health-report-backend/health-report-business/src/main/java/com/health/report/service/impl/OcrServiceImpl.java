package com.health.report.service.impl;

import com.health.report.config.OcrProperties;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.mapper.CollectionFieldConfigMapper;
import com.health.report.service.IOcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OcrServiceImpl implements IOcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceImpl.class);
    private static final int BATCH_SIZE = 5;

    // 单位匹配（忽略大小写）
    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "^(mmol/L|μmol/L|U/L|g/L|mg/dL|%/HP|10\\^\\d+/L|mmHg|cm|kg|岁|mmol/L|umol/L|mg/L|s|ng/ml)$",
            Pattern.CASE_INSENSITIVE
    );

    // 过滤参考范围行（修复正则）
    private static final Pattern RANGE_PATTERN = Pattern.compile(".*[~～\\-–—].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+\\.?\\d*");

    // 定性结果
    private static final Set<String> QUALITATIVE_WORDS = Set.of(
            "阴性", "阳性", "弱阳性", "强阳性", "无", "未见", "有", "少许", "大量", "偶见", "正常", "异常", "可疑"
    );

    // 通用垃圾词
    private static final Set<String> COMMON_WORDS = Set.of(
            "检查", "检验", "报告", "日期", "医生", "单位", "项目", "结果", "参考", "范围",
            "标识", "小结", "姓名", "性别", "年龄", "执行科室", "检验科", "临床生化", "申请时间", "报告时间"
    );

    @Autowired
    private CollectionFieldConfigMapper fieldConfigMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("ocrTaskExecutor")
    private Executor ocrTaskExecutor;

    @Autowired
    private OcrProperties ocrProperties;

    @Override
    public String recognize(MultipartFile file) throws Exception {
        List<MultipartFile> files = Collections.singletonList(file);
        List<Map<String, Object>> results = recognizeBatch(files);
        if (!results.isEmpty()) {
            Map<String, Object> result = results.get(0);
            if ((Boolean) result.getOrDefault("success", false)) {
                return (String) result.get("text");
            }
        }
        return "";
    }

    @Override
    public List<Map<String, Object>> recognizeBatch(List<MultipartFile> files) throws Exception {
        if (files == null || files.isEmpty()) return Collections.emptyList();
        List<MultipartFile> validFiles = files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (validFiles.isEmpty()) return Collections.emptyList();

        List<List<MultipartFile>> batches = partition(validFiles, BATCH_SIZE);
        List<CompletableFuture<List<Map<String, Object>>>> futures = new ArrayList<>();

        for (List<MultipartFile> batch : batches) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return callOcrApi(batch);
                } catch (Exception e) {
                    log.error("OCR批量识别异常", e);
                    return Collections.emptyList();
                }
            }, ocrTaskExecutor));
        }

        List<Map<String, Object>> finalResult = new ArrayList<>();
        for (CompletableFuture<List<Map<String, Object>>> future : futures) {
            finalResult.addAll(future.join());
        }
        return finalResult;
    }

    private List<Map<String, Object>> callOcrApi(List<MultipartFile> files) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            body.add("files", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<List> response = restTemplate.postForEntity(ocrProperties.getServerUrl(), new HttpEntity<>(body, headers), List.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Object obj : response.getBody()) {
                Map<String, Object> map = (Map<String, Object>) obj;
                resultList.add(map);
            }
            return resultList;
        }
        throw new Exception("OCR服务调用失败：" + response.getStatusCode());
    }

    @Override
    public Map<String, Object> extractIndicators(List<MultipartFile> files, Long collectionId) throws Exception {
        List<CollectionFieldConfig> configList = fieldConfigMapper.selectCollectionFieldConfigList(null);

        Map<String, CollectionFieldConfig> fieldMap = new HashMap<>();
        for (CollectionFieldConfig c : configList) {
            fieldMap.put(c.getFieldCode().toLowerCase(), c);
            if (c.getFieldName() != null) fieldMap.put(c.getFieldName().toLowerCase(), c);
            if (c.getFieldNameEn() != null) fieldMap.put(c.getFieldNameEn().toLowerCase(), c);
            if (c.getRemark() != null) fieldMap.put(c.getRemark().toLowerCase(), c);
            if (c.getRemarkEn() != null) fieldMap.put(c.getRemarkEn().toLowerCase(), c);

            if (c.getAliasName() != null && !c.getAliasName().isBlank()) {
                for (String s : c.getAliasName().split("[,，、;]")) {
                    fieldMap.put(s.trim().toLowerCase(), c);
                }
            }
            if (c.getAliasNameEn() != null && !c.getAliasNameEn().isBlank()) {
                for (String s : c.getAliasNameEn().split("[,，、;]")) {
                    fieldMap.put(s.trim().toLowerCase(), c);
                }
            }
        }

        List<Map<String, Object>> resultList = recognizeBatch(files);
        List<Map<String, Object>> matched = new ArrayList<>();
        Map<String, Object> extracted = new HashMap<>();

        for (Map<String, Object> res : resultList) {
            if (!Boolean.TRUE.equals(res.get("success"))) continue;
            String text = (String) res.get("text");
            if (text == null) continue;

            List<Map<String, Object>> items = parseTextToIndicators(text, fieldMap);
            for (Map<String, Object> item : items) {
                item.put("sourceFile", res.get("filename"));
                matched.add(item);
                extracted.put((String) item.get("fieldCode"), item.get("value"));
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("extractedData", extracted);
        resp.put("matchedIndicators", matched);
        resp.put("totalMatched", matched.size());
        return resp;
    }

    private List<Map<String, Object>> parseTextToIndicators(String text, Map<String, CollectionFieldConfig> fieldMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        text = text.replaceAll("\\s+", " ").trim();
        String[] tokens = text.split(" ");

        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].trim();
            if (word.isBlank() || COMMON_WORDS.contains(word) || RANGE_PATTERN.matcher(word).matches()) {
                continue;
            }

            CollectionFieldConfig field = fieldMap.get(word.toLowerCase());
            if (field == null) continue;

            BigDecimal value = null;
            String unit = null;

            for (int j = i + 1; j < Math.min(i + 6, tokens.length); j++) {
                String v = tokens[j].trim();
                Matcher m = NUMBER_PATTERN.matcher(v);
                if (m.find()) {
                    try {
                        value = new BigDecimal(m.group());
                        for (int k = j + 1; k < Math.min(j + 3, tokens.length); k++) {
                            String u = tokens[k].trim();
                            if (isUnit(u)) {
                                unit = u;
                                break;
                            }
                        }
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }

            if (value != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("fieldCode", field.getFieldCode());
                map.put("fieldName", field.getFieldName());
                map.put("value", value);
                map.put("unit", unit != null ? unit : field.getUnit());
                map.put("originalText", word);
                result.add(map);
            }
        }

        return result.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("fieldCode"),
                        m -> m,
                        (existing, replace) -> existing
                ))
                .values().stream().toList();
    }

    private boolean isUnit(String s) {
        return s != null && UNIT_PATTERN.matcher(s.trim()).matches();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    @Override
    public String getEngineName() {
        return "ExternalOCR";
    }
}