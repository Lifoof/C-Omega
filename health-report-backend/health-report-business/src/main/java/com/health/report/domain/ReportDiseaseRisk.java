package com.health.report.domain;

import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 疾病风险结果表
 * 存储疾病预测模型的风险概率结果
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReportDiseaseRisk extends BaseEntity {

    /** 主键ID */
    private Long id;

    /** 报告ID */
    private Long reportId;

    /** 模型ID */
    private Long modelId;

    /** 模型名称 */
    private String modelName;

    /** 疾病名称 */
    private String diseaseName;

    /** 疾病名称英文 */
    private String diseaseNameEn;

    /** 十年风险概率（0-1之间的小数） */
    private BigDecimal riskProbability;

    /** 风险等级：LOW低 MEDIUM中 HIGH高 */
    private String riskLevel;

    /** 原始结果JSON */
    private String rawResult;

    /**排序 */
    private Integer sortOrder;
}
