package com.health.report.dto.model;

import com.health.report.common.annotation.Excel;
import lombok.Data;

@Data
public class FieldParamImport {
    @Excel(name = "指标名称")
    private String fieldName;
}
