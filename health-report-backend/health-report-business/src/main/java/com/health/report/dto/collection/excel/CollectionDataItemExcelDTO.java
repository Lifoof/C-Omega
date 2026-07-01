package com.health.report.dto.collection.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel明细表（各分类采集项）解析DTO
 */
@Data
public class CollectionDataItemExcelDTO {
    // Excel列：字段编码（必填，与后端fieldConfig一致）
    @ExcelProperty(value = "字段编码", index = 0)
    private String fieldCode;

    // Excel列：字段名称（必填，冗余存储）
    @ExcelProperty(value = "字段名称", index = 1)
    private String fieldName;

    // Excel列：填写值（按字段类型填写，必填项不能为空）
    @ExcelProperty(value = "填写值", index = 2)
    private String fieldValue;

    // Excel列：是否必填（必填，1=是/0=否）
    @ExcelProperty(value = "是否必填", index = 3)
    private Integer isRequired;

    // Excel列：备注（可选）
    @ExcelProperty(value = "备注", index = 4)
    private String remark;

    // 非Excel字段：所属分类编码（由sheet名称映射）
    private String categoryCode;

    // 非Excel字段：关联的主表DTO（用于绑定collection_id）
    private CollectionRecordExcelDTO recordDTO;
}
