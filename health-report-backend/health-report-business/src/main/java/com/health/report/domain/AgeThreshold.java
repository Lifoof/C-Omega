package com.health.report.domain;

import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.health.report.common.annotation.Excel;
import com.health.report.common.core.domain.BaseEntity;

/**
 * 指标年龄阈值对象 age_threshold
 * 
 * @author ruoyi
 * @date 2026-04-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgeThreshold extends BaseEntity
{

    /** 主键ID */
    private Long id;

    /** 年龄 */
    @Excel(name = "年龄")
    private Integer age;

    /** 性别 1男 2女 */
    @Excel(name = "性别", dictType = "sys_user_sex")
    private Integer gender;

    /** 关联指标编码 */
    private String fieldCode;

    /** 关联指标名称 */
    @Excel(name = "关联指标名称")
    private String fieldName;

    private String fieldNameEn;

    /** 指标最小值 */
    @Excel(name = "指标最小值")
    private BigDecimal minVal;

    /** 指标最大值 */
    @Excel(name = "指标最大值")
    private BigDecimal maxVal;

    /** 单位 */
    @Excel(name = "单位")
    private String unit;
}
