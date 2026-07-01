package com.health.report.domain;

import com.health.report.common.annotation.Excel;
import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型结果指标明细表
 * 存储每个模型返回的临床指标、当前值、年龄偏差贡献等信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReportModelIndicator extends BaseEntity {

    /** 主键ID */
    private Long id;

    /** 报告ID */
    private Long reportId;

    /** 模型结果ID(report_model_result表) */
    private Long modelResultId;

    /** 模型优先级 */
    private Integer modelPriority;

    /** 模型ID */
    private Long modelId;

    /** 指标名称（如：不饱和铁结合力） */
    private String indicatorName;

    /** 指标名称英文 */
    private String indicatorNameEn;

    /** 指标编码（用于匹配参考范围） */
    private String indicatorCode;

    /** 当前值 */
    private BigDecimal currentValue;

    /** 当前值字符串（带单位，如：1.0） */
    private String currentValueStr;

    /** 同龄人参考范围（如：1.0-1.55） */
    private String referenceRange;

    /** 年龄偏差贡献（岁） */
    private BigDecimal ageContribution;

    /** 单位（如：mmol/L） */
    private String unit;

    /** 单位英文 */
    private String unitEn;

    /** 排序（按贡献度绝对值降序） */
    private Integer sortOrder;

    /** 指标最小值 */
    @Excel(name = "指标最小值")
    private BigDecimal minVal;

    /** 指标最大值 */
    @Excel(name = "指标最大值")
    private BigDecimal maxVal;
}
