package com.health.report.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.health.report.common.annotation.Excel;
import com.health.report.common.core.domain.BaseEntity;

/**
 * 模型参数关联（关键+全部参数共用）对象 ai_model_param
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelParam extends BaseEntity
{

    /** 主键 */
    private Long id;

    /** 模型ID */
    @Excel(name = "模型ID")
    private Long modelId;

    /** 参数ID */
    @Excel(name = "参数ID")
    private Long paramId;

    /** 参数类型：required=必填/关键，optional=可选/全部 */
    @Excel(name = "参数类型：required=必填/关键，optional=可选/全部")
    private String paramType;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sort;

}
