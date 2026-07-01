package com.health.report.service.impl;

import com.health.report.common.utils.StringUtils;
import com.health.report.domain.*;
import com.health.report.dto.collection.HealthReportData;
import com.health.report.mapper.*;
import com.health.report.utils.ChartGeneratorUtil;
import com.health.report.utils.PdfGeneratorUtil;
import com.health.report.utils.PythonScriptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportPdfServiceImpl {

    @Autowired
    private PdfGeneratorUtil pdfGeneratorUtil;
    @Autowired
    private final ChartGeneratorUtil chartGeneratorUtil;
    @Autowired
    private final ReportMapper reportMapper;
    @Autowired
    private final CollectionRecordMapper collectionRecordMapper;
    @Autowired
    private final MemberInfoMapper memberInfoMapper;
    @Autowired
    private final ReportModelResultMapper reportModelResultMapper;
    @Autowired
    private final ReportModelIndicatorMapper reportModelIndicatorMapper;
    @Autowired
    private final ReportDiseaseRiskMapper reportDiseaseRiskMapper;
    @Autowired
    private final PythonScriptClient pythonScriptClient;

    @Value("${ruoyi.profile}")
    private String profile;

    private static final DateTimeFormatter DT_FORMAT_ZH = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss");
    private static final DateTimeFormatter DT_FORMAT_EN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT_ZH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMAT_EN = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String OVERALL_ZH = "整体情况";
    private static final String OVERALL_EN = "Overall";

    /**
     * 生成PDF（中英文双版本）
     */
    public String generatePdfReport(Long reportId) {
        log.info("[PDF生成] 开始 reportId:{}", reportId);

        Report report = Optional.ofNullable(reportMapper.selectReportById(reportId))
                .orElseThrow(() -> new RuntimeException("报告不存在"));

        CollectionRecord record = Optional.ofNullable(collectionRecordMapper.selectById(report.getCollectionId()))
                .orElseThrow(() -> new RuntimeException("采集记录不存在"));

        MemberInfo member = memberInfoMapper.selectById(record.getMemberId());

        try {
            String zhPath = generatePdfByLanguage(report, record, member, "zh");
            String enPath = generatePdfByLanguage(report, record, member, "en");

            report.setFilePath(zhPath);
            report.setFilePathEn(enPath);
            reportMapper.updateReport(report);

            log.info("[PDF生成] 完成 reportId:{}", reportId);
            return zhPath;
        } catch (Exception e) {
            log.error("[PDF生成] 失败 reportId:{}", reportId, e);
            throw new RuntimeException("PDF生成失败：" + e.getMessage());
        }
    }

    private String generatePdfByLanguage(Report report, CollectionRecord record, MemberInfo member, String lang) {
        boolean en = "en".equals(lang);
        Long reportId = report.getId();

        log.info("[PDF生成] 生成{}版 reportId:{}", en ? "英文" : "中文", reportId);

        HealthReportData data = buildReportData(report, record, member, lang);
        List<String> systemLabels = pythonScriptClient.getSystemTypeLabels(lang);

        String radarBase64 = chartGeneratorUtil.generateRadarChartBase64(
                data.getRadarChartData(lang),
                data.getAge(),
                systemLabels,lang
        );
        data.setRadarChartBase64(radarBase64);

        String templateName = en ? "health-report-template-en.html" : "health-report-template.html";
        String html = pdfGeneratorUtil.loadHtmlTemplate(templateName);
        String filled = fillTemplate(html, data, lang);

        String dirPath = profile + "/reports/";
        makeDirIfNotExists(dirPath);

        String fileName = "report_%s_%s_%s.pdf".formatted(
                report.getReportNo(),
                lang,
                UUID.randomUUID().toString().replace("-", "")
        );
        String pdfPath = dirPath + fileName;

        pdfGeneratorUtil.generatePdfFromHtml(filled, pdfPath,true,en);
        return "/profile/reports/" + fileName;
    }

    private HealthReportData buildReportData(Report report, CollectionRecord record, MemberInfo member, String lang) {
        HealthReportData data = new HealthReportData();
        boolean en = "en".equals(lang);
        Long reportId = report.getId();
        int age = calculateAgeSafely(member);
        BigDecimal ageBd = new BigDecimal(age);

        data.setReportNo(report.getReportNo());
        data.setReportTime(LocalDateTime.now());
        data.setMemberId(StringUtils.hasText(member != null ? member.getMemberNo() : null)
                ? member.getMemberNo()
                : record.getMemberName());
        data.setMemberName(member != null ? member.getName() : record.getMemberName());
        data.setGender(Objects.equals(member != null ? member.getGender() : null, 1)
                ? (en ? "Male" : "男")
                : (en ? "Female" : "女"));
        data.setAge(age);
        data.setCheckupDate(record.getCheckupDate() != null ? record.getCheckupDate() : LocalDate.now());
        data.setHospital(record.getHospital());
        data.setCompleteness(report.getCompleteness());
        data.setModelName(report.getModelName());
        data.setModelNameEn(report.getModelNameEn());
        data.setLanguage(lang);

        fillBiologyAges(data, reportId, ageBd, lang);
        fillIndicators(data, reportId, member, age, lang);
        fillDiseaseRisks(data, reportId, lang);
        generateConclusions(data, age, reportId);

        return data;
    }

    private void fillBiologyAges(HealthReportData data, Long reportId, BigDecimal actual, String lang) {
        boolean en = "en".equals(lang);
        List<ReportModelResult> results = reportModelResultMapper.selectByReportId(reportId);

        if (results != null) {
            results.stream()
                    .filter(r -> !"disease".equals(r.getCategory()))
                    .forEach(r -> {
                        String label = getCategoryLabel(r.getCategory(), lang);
                        if (r.getBiologyAge() != null) {
                            data.getBiologyAges().put(label, r.getBiologyAge());
                        }
                    });
        }

        if (data.getBiologyAges().isEmpty()) {
            List<String> list = en
                    ? List.of("Overall", "Cardiovascular", "Kidney", "Endocrine", "Lung",
                    "Digestive", "Immune", "Blood", "Bone", "Nutrition")
                    : List.of("整体情况", "心血管系统", "肾脏", "内分泌系统", "呼吸系统",
                    "消化系统", "免疫系统", "造血系统", "骨骼肌肉系统", "营养评估");
            list.forEach(s -> data.getBiologyAges().put(s, actual));
        }
    }

    private void fillIndicators(HealthReportData data, Long reportId, MemberInfo member, int age, String lang) {
        boolean en = "en".equals(lang);
        Integer gender = member != null ? member.getGender() : 1;

        List<ReportModelIndicator> list = reportModelIndicatorMapper.selectIndicatorsWithDetails(reportId, gender, age);
        if (list != null && !list.isEmpty()) {
            list.forEach(ind -> {
                String name = en && StringUtils.hasText(ind.getIndicatorNameEn())
                        ? ind.getIndicatorNameEn()
                        : ind.getIndicatorName();

                String unit = en && StringUtils.hasText(ind.getUnitEn())
                        ? ind.getUnitEn()
                        : ind.getUnit();

                String refRange = "";
                if (ind.getMinVal() != null && ind.getMaxVal() != null) {
                    refRange = "%.2f-%.2f".formatted(ind.getMinVal(), ind.getMaxVal());
                    if (StringUtils.hasText(unit)) refRange += " " + unit;
                } else if (StringUtils.hasText(ind.getReferenceRange())) {
                    refRange = ind.getReferenceRange();
                }

                data.getIndicators().add(
                        createIndicator(
                                name,
                                //去掉末尾无效的0
                                ind.getCurrentValue() == null ? "-" : ind.getCurrentValue().stripTrailingZeros().toPlainString(),
                                unit,
                                refRange,
                                ind.getAgeContribution() != null ? ind.getAgeContribution() : BigDecimal.ZERO,
                                "",
                                BigDecimal.ZERO,
                                "NORMAL"
                        )
                );
            });
        }

    }

    private void fillDiseaseRisks(HealthReportData data, Long reportId, String lang) {
        boolean en = "en".equals(lang);
        List<ReportDiseaseRisk> risks = reportDiseaseRiskMapper.selectByReportId(reportId);

        if (risks != null) {
            risks.forEach(r -> {
                String name = en ? getDiseaseEn(r.getDiseaseName()) : r.getDiseaseName();
                data.getDiseaseRisks().add(
                        createDiseaseRisk(
                                name,
                                r.getRiskProbability() != null ? r.getRiskProbability() : BigDecimal.ZERO,
                                r.getRiskLevel() != null ? r.getRiskLevel() : "LOW"
                        )
                );
            });
        }

        if (data.getDiseaseRisks().isEmpty()) {
            List<String> defaultDis = en
                    ? List.of("Coronary Heart Disease", "Myocardial Infarction", "Heart Failure", "Stroke", "Diabetes", "Chronic Kidney Disease", "Lung Cancer")
                    : List.of("冠心病", "心肌梗死", "心力衰竭", "脑卒中", "糖尿病", "慢性肾病", "肺癌");
            defaultDis.forEach(s -> data.getDiseaseRisks().add(createDiseaseRisk(s, BigDecimal.ZERO, "LOW")));
        }
    }

    private void generateConclusions(HealthReportData data, int actualAge, Long reportId) {
        boolean en = "en".equals(data.getLanguage());
        String overall = en ? OVERALL_EN : OVERALL_ZH;
        BigDecimal overallAge = data.getBiologyAges().get(overall);

        if (overallAge != null) {
            double gap = overallAge.doubleValue() - actualAge;
            if (gap != 0) {
                if (en) {
                    data.getConclusions().add("Your overall biological age (%.1f) is %s than actual age (%d) by %.1f years."
                            .formatted(overallAge.doubleValue(), gap < 0 ? "younger" : "older", actualAge, Math.abs(gap)));
                } else {
                    data.getConclusions().add("整体生物学年龄(%.1f岁)比实际年龄(%d岁)%s%.1f岁。"
                            .formatted(overallAge.doubleValue(), actualAge, gap < 0 ? "年轻" : "年长", Math.abs(gap)));
                }
            }
        }

        List<String> accelerate = new ArrayList<>();
        List<String> decelerate = new ArrayList<>();

        data.getBiologyAges().forEach((k, v) -> {
            if (k.equals(overall)) return;
            double g = v.doubleValue() - actualAge;
            if (g > 0) accelerate.add(k);
            else if (g < 0) decelerate.add(k);
        });

        if (!accelerate.isEmpty()) {
            if (en) data.getConclusions().add("Aging accelerated systems: " + String.join(", ", accelerate));
            else data.getConclusions().add("衰老加速系统：" + String.join("、", accelerate));
        }
        if (!decelerate.isEmpty()) {
            if (en) data.getConclusions().add("Aging decelerated systems: " + String.join(", ", decelerate));
            else data.getConclusions().add("衰老减速系统：" + String.join("、", decelerate));
        }

        List<String> topIndicators = getTop7Indicators(reportId, data.getLanguage());
        if (!topIndicators.isEmpty()) {
            if (en) {
                data.getConclusions().add("Factors that significantly contribute to your accelerated aging include: " + String.join(", ", topIndicators));
            } else {
                data.getConclusions().add("对您的衰老加速影响较大的因素包括：" + String.join("、", topIndicators));
            }
        }
    }

    private List<String> getTop7Indicators(Long reportId, String lang) {
        boolean en = "en".equals(lang);
        try {
            ReportModelResult top = reportModelResultMapper.selectTopPriorityByReportId(reportId);
            if (top == null) return Collections.emptyList();

            List<ReportModelIndicator> list = reportModelIndicatorMapper.selectByReportIdAndModelId(reportId, top.getModelId());
            if (list == null || list.isEmpty()) return Collections.emptyList();

            return list.stream()
                    .limit(7)
                    .map(i -> en && StringUtils.hasText(i.getIndicatorNameEn()) ? i.getIndicatorNameEn() : i.getIndicatorName())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[获取Top指标异常] reportId:{}", reportId, e);
            return Collections.emptyList();
        }
    }

    private String getCategoryLabel(String category, String lang) {
        boolean en = "en".equals(lang);
        if (category == null) return en ? "Other" : "其他";
        return switch (category) {
            case "whole" -> en ? "Overall" : "整体情况";
            case "cardiovascular" -> en ? "Cardiovascular" : "心血管系统";
            case "kidney" -> en ? "Kidney" : "肾脏";
            case "endocrine" -> en ? "Endocrine" : "内分泌系统";
            case "lung" -> en ? "Respiratory" : "呼吸系统";
            case "digestive" -> en ? "Digestive" : "消化系统";
            case "immune" -> en ? "Immune" : "免疫系统";
            case "blood" -> en ? "Blood" : "造血系统";
            case "bone" -> en ? "Bone" : "骨骼肌肉系统";
            case "nutrition" -> en ? "Nutrition" : "营养评估";
            default -> category;
        };
    }

    private String getDiseaseEn(String cn) {
        if (cn == null) return "";
        return switch (cn) {
            case "冠心病" -> "Coronary Heart Disease";
            case "心肌梗死" -> "Myocardial Infarction";
            case "心力衰竭" -> "Heart Failure";
            case "脑卒中" -> "Stroke";
            case "糖尿病" -> "Diabetes";
            case "慢性肾病" -> "Chronic Kidney Disease";
            case "肺癌" -> "Lung Cancer";
            case "肺气肿" -> "Emphysema";
            case "高血压" -> "Hypertension";
            case "心功能不全" -> "Cardiac Insufficiency";
            case "主动脉粥样硬化" -> "Aortic Atherosclerosis";
            default -> cn;
        };
    }

    private HealthReportData.IndicatorItem createIndicator(String name, String value, String unit, String ref,
                                                           BigDecimal gap, String disease, BigDecimal risk, String status) {
        HealthReportData.IndicatorItem item = new HealthReportData.IndicatorItem();
        item.setName(name);
        item.setCurrentValue(value);
        item.setUnit(unit);
        item.setRefRange(ref);
        item.setAgeGap(gap);
        item.setDisease(disease);
        item.setRiskProbability(risk);
        item.setStatus(status);
        return item;
    }

    private HealthReportData.DiseaseRiskItem createDiseaseRisk(String name, BigDecimal prob, String level) {
        HealthReportData.DiseaseRiskItem item = new HealthReportData.DiseaseRiskItem();
        item.setDiseaseName(name);
        item.setProbability(prob);
        item.setRiskLevel(level);
        return item;
    }

    private Integer calculateAgeSafely(MemberInfo member) {
        try {
            if (member == null || member.getBirthDate() == null) return 45;
            LocalDate birth = member.getBirthDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            return LocalDate.now().getYear() - birth.getYear();
        } catch (Exception e) {
            return 45;
        }
    }

    private void makeDirIfNotExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }

    private String fillTemplate(String template, HealthReportData data, String lang) {
        boolean en = "en".equals(lang);
        DateTimeFormatter dtf = en ? DT_FORMAT_EN : DT_FORMAT_ZH;
        DateTimeFormatter df = en ? DATE_FORMAT_EN : DATE_FORMAT_ZH;

        return template
                .replace("${reportTime}", data.getReportTime().format(dtf))
                .replace("${memberId}", StringUtils.hasText(data.getMemberId()) ? data.getMemberId() : "DEMO_0452")
                .replace("${gender}", data.getGender())
                .replace("${age}", String.valueOf(data.getAge()))
                .replace("${checkupDate}", data.getCheckupDate().format(df))
                .replace("${biologyAgeRows}", data.getBiologyAgeRowsHtml(lang))
                .replace("${radarChartBase64}", data.getRadarChartBase64())
                .replace("${indicatorRows}", data.getIndicatorRowsHtml(lang))
                .replace("${diseaseRows}", data.getDiseaseRowsHtml(lang))
                .replace("${conclusionItems}", data.getConclusionItemsHtml(lang));
    }
}