package com.health.report.domain;

import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 体检采集数据明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionDataItem extends BaseEntity {

    /**
     * 明细ID（主键自增）
     */
    private Long id;

    /**
     * 采集记录ID
     */
    private Long collectionId;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 指标编码
     */
    private String fieldCode;

    /**
     * 指标名称
     */
    private String fieldName;

    /**
     * 数值
     */
    private BigDecimal numberValue;

    /**
     * 字符串值
     */
    private String stringValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 是否异常（0=正常 1=异常）
     */
    private Integer isAbnormal;

    /**
     * 异常等级（0=正常 1=轻度 2=中度 3=重度）
     */
    private Integer abnormalLevel;

    /**
     * 删除标记（0=正常 1=删除）
     */
    private Integer deleted;


}
