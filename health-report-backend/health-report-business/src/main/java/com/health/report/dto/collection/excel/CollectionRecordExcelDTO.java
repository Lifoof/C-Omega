package com.health.report.dto.collection.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

/**
 * Excel主表（个人信息）解析DTO
 */
@Data
public class CollectionRecordExcelDTO {
    // Excel列：用户编号（必填）
    @ExcelProperty(value = "用户编号", index = 1)
    private String memberNo;
    // Excel列：用户姓名（必填）
    @ExcelProperty(value = "用户姓名", index = 2)
    private String memberName;
    // Excel列：性别（必填，1=男/2=女）
    @ExcelProperty(value = "性别", index = 3)
    private Integer memberGender;
    // Excel列：体检日期（必填，格式YYYY-MM-DD）
    @ExcelProperty(value = "体检日期", index = 4)
    private LocalDate checkupDate;
    /** 采集字段值（key=Excel列名，value=字段值） */
    private Map<String, String> fieldValues;
    // 非Excel字段：Excel行号（用于标记失败位置）
    private Integer rowNum;
}
