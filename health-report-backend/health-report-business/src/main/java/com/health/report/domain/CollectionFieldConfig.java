package com.health.report.domain;

import com.health.report.common.annotation.Excel;
import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionFieldConfig extends BaseEntity {

    /** 主键ID */
    private Long id;

    /** 字段编码 */
    private String fieldCode;
    /** 归属类别 */
    @Excel(name = "归属类别")
    private String categoryName;
    /** 归属类别英文 */
    @Excel(name = "归属类别英文")
    private String categoryNameEn;
    /** 字段中文名 */
    @Excel(name = "指标名称")
    private String fieldName;
    /** 模型参数名称 */
    @Excel(name = "模型参数名称")
    private String fieldParamName;
    /** 指标名称英文 */
    @Excel(name = "指标名称英文")
    private String fieldNameEn;
    /** 单位 */
    @Excel(name = "指标单位")
    private String unit;
    /** 单位 */
    @Excel(name = "指标单位英文")
    private String unitEn;
    /** 所属分组 */
    private String categoryCode;
    /** 性别范围：0通用 1女性 2男性 */
    private Integer genderScope;
    /** 数据类型：NUMBER/TEXT/ENUM/BOOLEAN */
    private String fieldType;
    /** 正常范围文字描述 */
    @Excel(name = "参考值/范围")
    private String refRangeText;
    /** 正常范围文字描述 */
    @Excel(name = "参考值/范围英文")
    private String refRangeTextEn;

    /** 男性参考最小值 */
    private BigDecimal refMinMale;

    /** 男性参考最大值 */
    private BigDecimal refMaxMale;

    /** 女性参考最小值 */
    private BigDecimal refMinFemale;

    /** 女性参考最大值 */
    private BigDecimal refMaxFemale;

    /** 字段说明 */
    private String description;

    /** 是否必填(关键字段) */
    private Integer isRequired;

    /** JSON数组（枚举选项） */
    private String enumOptions;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0禁用1启用 */
    private Integer status;

    /** 删除标志（0代表存在 2代表删除） */
    private Integer deleted;
    /** 别名英文 */
    @Excel(name = "备注")
    private String remark;
    /** 别名英文 */
    @Excel(name = "备注英文")
    private String remarkEn;
    /** 别名 */
    @Excel(name = "别名")
    private String aliasName;
    /** 别名英文 */
    @Excel(name = "别名英文")
    private String aliasNameEn;


}
