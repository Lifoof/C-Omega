package com.health.report.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.health.report.common.annotation.Excel;
import com.health.report.common.core.domain.BaseEntity;

/**
 * JSON记录对象 json_record
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JsonRecord extends BaseEntity
{


    /** 主键ID */
    private Long id;

    /** 模型ID */
    @Excel(name = "模型ID")
    private Long modelId;

    /** 报告ID */
    @Excel(name = "报告ID")
    private Long reportId;

    /** JSON内容 */
    @Excel(name = "JSON内容")
    private String jsonContent;

    /** 数据类型 1=传参 2=返回 */
    @Excel(name = "数据类型 1=传参 2=返回")
    private Integer dataType;

    /** 删除标记 */
    @Excel(name = "删除标记")
    private Integer deleted;

}
