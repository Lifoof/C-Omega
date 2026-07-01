package com.health.report.dto.collection;

import lombok.Data;

import java.util.Map;

@Data
public class CategorySaveDTO {
    private String categoryCode;
    private Map<String, Object> fieldData;
}
