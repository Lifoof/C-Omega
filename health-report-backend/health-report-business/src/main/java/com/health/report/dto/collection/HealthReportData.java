package com.health.report.dto.collection;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 健康报告数据DTO
 * 用于填充HTML模板
 */
@Data
public class HealthReportData {

    /** 报告编号 */
    private String reportNo;

    /** 报告生成时间 */
    private LocalDateTime reportTime;

    /** 受检者ID */
    private String memberId;

    /** 受检者姓名 */
    private String memberName;

    /** 性别 */
    private String gender;

    /** 年龄 */
    private Integer age;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 体检机构 */
    private String hospital;

    /** 数据完整度 */
    private BigDecimal completeness;

    /** 模型名称 */
    private String modelName;
    /** 模型英文名称 */
    private String modelNameEn;

    /** 语言: zh/en */
    private String language = "zh";

    /** 生物学年龄数据（系统名称 -> 生物学年龄） */
    private Map<String, BigDecimal> biologyAges = new HashMap<>();

    /** 雷达图Base64 */
    private String radarChartBase64;

    /** 详细指标列表 */
    private List<IndicatorItem> indicators = new ArrayList<>();

    /** 结论列表 */
    private List<String> conclusions = new ArrayList<>();

    /** 疾病风险列表 */
    private List<DiseaseRiskItem> diseaseRisks = new ArrayList<>();

    // 系统标签顺序（与参考图片一致）
    private static final String[] SYSTEM_ORDER = {
            "整体情况", "心血管系统", "营养评估", "消化系统", "呼吸系统",
            "造血系统", "免疫系统", "肾脏", "内分泌系统", "骨骼肌肉系统"
    };

    // 英文系统标签顺序
    private static final String[] SYSTEM_ORDER_EN = {
            "Overall", "Cardiovascular", "Nutrition", "Digestive", "Respiratory",
            "Blood", "Immune", "Kidney", "Endocrine", "Bone"
    };

    // 中文到英文系统名称映射
    private static final Map<String, String> SYSTEM_NAME_EN_MAP = new HashMap<>();
    static {
        SYSTEM_NAME_EN_MAP.put("整体情况", "Overall");
        SYSTEM_NAME_EN_MAP.put("心血管系统", "Cardiovascular");
        SYSTEM_NAME_EN_MAP.put("肾脏", "Kidney");
        SYSTEM_NAME_EN_MAP.put("内分泌系统", "Endocrine");
        SYSTEM_NAME_EN_MAP.put("呼吸系统", "Respiratory");
        SYSTEM_NAME_EN_MAP.put("消化系统", "Digestive");
        SYSTEM_NAME_EN_MAP.put("免疫系统", "Immune");
        SYSTEM_NAME_EN_MAP.put("造血系统", "Blood");
        SYSTEM_NAME_EN_MAP.put("骨骼肌肉系统", "Bone");
        SYSTEM_NAME_EN_MAP.put("营养评估", "Nutrition");
    }

    /**
     * 指标项
     */
    @Data
    public static class IndicatorItem {
        /** 指标名称 */
        private String name;

        /** 当前值 */
        private String currentValue;

        /** 单位 */
        private String unit;

        /** 参考范围 */
        private String refRange;

        /** 年龄偏差贡献（岁） */
        private BigDecimal ageGap;

        /** 相关疾病/事件 */
        private String disease;

        /** 10年风险概率 */
        private BigDecimal riskProbability;

        /** 状态：NORMAL-正常, HIGH-偏高, LOW-偏低 */
        private String status;
    }

    /**
     * 获取生物学年龄表格行HTML - 匹配参考图片格式
     * 列顺序：系统/器官 | 生物学年龄 | Age Gap
     */
    public String getBiologyAgeRowsHtml() {
        return getBiologyAgeRowsHtml(language);
    }

    public String getBiologyAgeRowsHtml(String lang) {
        StringBuilder sb = new StringBuilder();
        boolean isEnglish = "en".equals(lang);
        String[] systemOrder = isEnglish ? SYSTEM_ORDER_EN : SYSTEM_ORDER;

        for (String system : systemOrder) {
            // 直接使用系统名称查找数据（buildReportData 已经根据语言存储了对应名称）
            BigDecimal bioAge = biologyAges.getOrDefault(system, BigDecimal.valueOf(age));
            BigDecimal gap = bioAge.subtract(BigDecimal.valueOf(age));
            sb.append(buildAgeRow(system, bioAge, gap, lang));
        }

        return sb.toString();
    }

    private String getChineseSystemName(String englishName) {
        for (Map.Entry<String, String> entry : SYSTEM_NAME_EN_MAP.entrySet()) {
            if (entry.getValue().equals(englishName)) {
                return entry.getKey();
            }
        }
        return englishName;
    }

    private String buildAgeRow(String system, BigDecimal bioAge, BigDecimal gap, String lang) {
        String gapClass = gap.compareTo(BigDecimal.ZERO) > 0 ? "age-gap-positive" : "age-gap-negative";
        String gapSign = gap.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        boolean isEnglish = "en".equals(lang);
        String ageUnit = isEnglish ? "yrs" : "岁";

        // 生物学年龄文本：等于实际年龄就显示NA
        String bioAgeText;
        if (bioAge.compareTo(BigDecimal.valueOf(age)) == 0) {
            bioAgeText = "NA";
        } else {
            bioAgeText = String.format("%.1f %s", bioAge.doubleValue(), ageUnit);
        }

        // 年龄偏差文本：差值为0显示NA
        String gapText;
        if (gap.compareTo(BigDecimal.ZERO) == 0) {
            gapText = "NA";
        } else {
            gapText = String.format("%s%.1f %s", gapSign, gap.doubleValue(), ageUnit);
        }

        return String.format(
                "<tr>" +
                        "<td class=\"system-name\">%s</td>" +
                        "<td class=\"age-highlight\">%s</td>" +
                        "<td class=\"%s\">%s</td>" +
                        "</tr>",
                system, bioAgeText, gapClass, gapText
        );
    }



    /**
     * 获取结论列表HTML
     */
    public String getConclusionItemsHtml() {
        return getConclusionItemsHtml(language);
    }

    public String getConclusionItemsHtml(String lang) {
        StringBuilder sb = new StringBuilder();
        for (String conclusion : conclusions) {
            sb.append("<li>").append(conclusion).append("</li>");
        }
        return sb.toString();
    }

    /**
     * 获取合并后的详细指标和疾病风险表格行HTML
     * 将整体衰老驱动指标和未来10年疾病风险预测合并到一个表格中
     */
    public String getCombinedRowsHtml() {
        StringBuilder sb = new StringBuilder();
        int maxRows = Math.max(indicators.size(), diseaseRisks.size());

        for (int i = 0; i < maxRows; i++) {
            // 获取指标数据（如果存在）
            IndicatorItem indicator = i < indicators.size() ? indicators.get(i) : null;
            // 获取疾病风险数据（如果存在）
            DiseaseRiskItem diseaseRisk = i < diseaseRisks.size() ? diseaseRisks.get(i) : null;

            sb.append(buildCombinedRow(indicator, diseaseRisk));
        }

        return sb.toString();
    }

    /**
     * 构建合并表格的一行
     */
    private String buildCombinedRow(IndicatorItem indicator, DiseaseRiskItem diseaseRisk) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");

        // 指标部分
        if (indicator != null) {
            String statusClass = "status-normal";
            if ("HIGH".equals(indicator.getStatus())) {
                statusClass = "status-high";
            } else if ("LOW".equals(indicator.getStatus())) {
                statusClass = "status-low";
            }

            // 构建年龄偏差字符串（带正负号和颜色）
            String ageGapStr = buildAgeGapWithColor(indicator.getAgeGap());


            sb.append(String.format(
                    "<td class=\"indicator-name\">%s</td>" +
                            "<td class=\"%s\">%s %s</td>" +
                            "<td class=\"ref-range\">%s</td>" +
                            "<td class=\"age-deviation\">%s</td>",
                    indicator.getName(),
                    statusClass,
                    indicator.getCurrentValue(),
                    indicator.getUnit() != null ? indicator.getUnit() : "",
                    indicator.getRefRange() != null ? indicator.getRefRange() : "-",
                    ageGapStr
            ));
        } else {
            // 空行填充
            sb.append("<td></td><td></td><td></td><td></td>");
        }

        // 疾病风险部分
        if (diseaseRisk != null) {
            String riskBadge = buildRiskBadge(diseaseRisk.getProbability(), diseaseRisk.getRiskLevel());
            sb.append(String.format(
                    "<td class=\"disease-name\">%s</td>" +
                            "<td>%s</td>",
                    diseaseRisk.getDiseaseName() != null ? diseaseRisk.getDiseaseName() : "-",
                    riskBadge
            ));
        } else {
            // 空行填充
            sb.append("<td></td><td></td>");
        }

        sb.append("</tr>");
        return sb.toString();
    }

    /**
     * 构建带颜色标记的年龄偏差字符串
     * 小于0：显示-号，岁字绿色
     * 大于0：显示+号，岁字红色
     * 等于0：不显示符号，岁字默认色
     */
    private String buildAgeGapWithColor(BigDecimal ageGap) {
        return buildAgeGapWithColor(ageGap, language);
    }

    private String buildAgeGapWithColor(BigDecimal ageGap, String lang) {
        if (ageGap == null) {
            return "-";
        }

        boolean isEnglish = "en".equals(lang);
        String ageUnit = isEnglish ? "yrs" : "岁";
        double value = ageGap.doubleValue();
        String sign = value > 0 ? "+" : (value < 0 ? "-" : "");
        String absValue = String.format("%.1f", Math.abs(value));

        if (value < 0) {
            // 负数：绿色岁字
            return String.format("%s%s<span class=\"age-unit-green\"> %s</span>", sign, absValue, ageUnit);
        } else if (value > 0) {
            // 正数：红色岁字
            return String.format("%s%s<span class=\"age-unit-red\"> %s</span>", sign, absValue, ageUnit);
        } else {
            // 零：默认颜色
            return String.format("%s<span class=\"age-unit\"> %s</span>", absValue, ageUnit);
        }
    }

    /**
     * 构建风险概率徽章HTML
     */
    private String buildRiskBadge(BigDecimal probability, String riskLevel) {
        return buildRiskBadge(probability, riskLevel, language);
    }

    private String buildRiskBadge(BigDecimal probability, String riskLevel, String lang) {
        if (probability == null) {
            return "<span class=\"risk-badge\">0.0%</span>";
        }

        double risk = probability.doubleValue() * 100; // 转换为百分比
        String badgeClass = "risk-badge";

        // 这里改成：大于等于 80 → 红色high，其他全部绿色
        if (risk >= 80) {
            badgeClass = "risk-badge risk-badge-high";
        }

        return String.format("<span class=\"%s\">%.1f%%</span>", badgeClass, risk);
    }

    /**
     * 获取整体衰老驱动指标表格行HTML（单独表格）
     * 与疾病风险表格行数保持一致
     */
    public String getIndicatorRowsHtml() {
        return getIndicatorRowsHtml(language);
    }

    public String getIndicatorRowsHtml(String lang) {
        StringBuilder sb = new StringBuilder();
        int maxRows = Math.max(indicators.size(), diseaseRisks.size());
        boolean isEnglish = "en".equals(lang);

        for (int i = 0; i < maxRows; i++) {
            if (i < indicators.size()) {
                IndicatorItem indicator = indicators.get(i);
                String statusClass = "status-normal";
                if ("HIGH".equals(indicator.getStatus())) {
                    statusClass = "status-high";
                } else if ("LOW".equals(indicator.getStatus())) {
                    statusClass = "status-low";
                }
                String ageGapStr = buildAgeGapWithColor(indicator.getAgeGap(), lang);

                sb.append(String.format(
                        "<tr>" +
                                "<td class=\"indicator-name\">%s</td>" +
                                "<td class=\"%s\">%s %s</td>" +
                                "<td class=\"ref-range\">%s</td>" +
                                "<td class=\"age-deviation\">%s</td>" +
                                "</tr>",
                        indicator.getName(),
                        statusClass,
                        indicator.getCurrentValue(),
                        indicator.getUnit() != null ? indicator.getUnit() : "",
                        indicator.getRefRange() != null ? indicator.getRefRange() : "-",
                        ageGapStr
                ));
            } else {
                // 填充空行以保持行数一致
                sb.append("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>");
            }
        }
        return sb.toString();
    }

    /**
     * 获取疾病风险表格行HTML（单独表格）
     * 与指标表格行数保持一致
     */
    public String getDiseaseRowsHtml() {
        return getDiseaseRowsHtml(language);
    }

    public String getDiseaseRowsHtml(String lang) {
        StringBuilder sb = new StringBuilder();
        int maxRows = Math.max(indicators.size(), diseaseRisks.size());

        for (int i = 0; i < maxRows; i++) {
            if (i < diseaseRisks.size()) {
                DiseaseRiskItem diseaseRisk = diseaseRisks.get(i);
                String riskBadge = buildRiskBadge(diseaseRisk.getProbability(), diseaseRisk.getRiskLevel(), lang);
                sb.append(String.format(
                        "<tr>" +
                                "<td class=\"disease-name\">%s</td>" +
                                "<td>%s</td>" +
                                "</tr>",
                        diseaseRisk.getDiseaseName() != null ? diseaseRisk.getDiseaseName() : "-",
                        riskBadge
                ));
            } else {
                // 填充空行以保持行数一致
                sb.append("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>");
            }
        }
        return sb.toString();
    }

    /**
     * 获取雷达图数据Map - 包含全部10个系统，缺失的用实际年龄填充
     */
    public Map<String, Double> getRadarChartData() {
        return getRadarChartData(language);
    }

    public Map<String, Double> getRadarChartData(String lang) {
        Map<String, Double> data = new LinkedHashMap<>();
        boolean isEnglish = "en".equals(lang);

        // 雷达图使用的系统顺序（与图片一致）
        String[] radarOrder = isEnglish ? SYSTEM_ORDER_EN : SYSTEM_ORDER;

        for (String system : radarOrder) {
            BigDecimal bioAge = biologyAges.get(system);
            // 如果该系统的生物学年龄不存在，使用实际年龄作为默认值
            if (bioAge == null || bioAge.compareTo(BigDecimal.ZERO) <= 0) {
                bioAge = BigDecimal.valueOf(age);
            }
            data.put(system, bioAge.doubleValue());
        }

        return data;
    }

    /**
     * 疾病风险项
     */
    @Data
    public static class DiseaseRiskItem {
        /** 疾病名称 */
        private String diseaseName;

        /** 风险概率（0-1） */
        private BigDecimal probability;

        /** 风险等级：LOW/MEDIUM/HIGH */
        private String riskLevel;
    }
}