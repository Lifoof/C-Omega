package com.health.report.domain;

import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiModel extends BaseEntity {

    private Long id;
    private String modelName;
    private String modelNameEn;
    private String modelCode;
    private String category;
    private Integer applicableGender;
    private String description;
    private String requiredParams;
    private Integer requiredParamCount;
    private String allParams;
    private Integer allParamCount;
    private Integer paramCount;
    private Integer callCount;
    private String pyPath;
    private String pklPath;
    private Integer status;
    private Integer deleted;
    /** 优先级：数字越小优先级越高 */
    private Integer priority;

    /** 已填写的关键参数数量（非数据库字段） */
    private Integer filledKeyParams;
    private String startDate;
    private String endDate;
}
