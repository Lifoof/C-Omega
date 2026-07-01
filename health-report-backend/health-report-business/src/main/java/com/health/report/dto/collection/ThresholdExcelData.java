package com.health.report.dto.collection;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 阈值宽表导入通用实体
 * 固定前两列：年龄、性别
 * 后面所有列动态解析，无需新增字段
 */
@Data
public class ThresholdExcelData {

    // 第0列 年龄
    @ExcelProperty(index = 0)
    private Integer age;

    // 第1列 性别
    @ExcelProperty(index = 1)
    private Integer gender;

}
