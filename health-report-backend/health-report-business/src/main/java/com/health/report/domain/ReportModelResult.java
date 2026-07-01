package com.health.report.domain;

import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报告模型结果实体
 * 存储每个模型调用返回的生物学年龄结果
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReportModelResult extends BaseEntity {

    /** ID */
    private Long id;

    /** 报告ID */
    private Long reportId;

    /** 模型ID */
    private Long modelId;

    /** 模型名称 */
    private String modelName;

    /** 模型编码 */
    private String modelCode;

    /** 系统类型分类（whole/cardiovascular等） */
    private String category;

    /** 模型优先级 */
    private Integer modelPriority;

    /** 模型优先级 */
    private Integer priority;

    /** 生物学年龄 */
    private BigDecimal biologyAge;

    /** 真实年龄 */
    private Integer actualAge;

    /** 年龄差距（生物学年龄-真实年龄） */
    private BigDecimal ageGap;

    /** 模型返回的原始JSON结果 */
    private String rawResult;

    /** 状态：0-失败，1-成功 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

}
