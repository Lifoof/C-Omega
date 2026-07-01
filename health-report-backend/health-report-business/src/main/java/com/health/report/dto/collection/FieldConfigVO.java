package com.health.report.dto.collection;

import lombok.Data;

@Data
public class FieldConfigVO {
    private Long id;
    private String fieldCode;
    private String fieldName;
    private String fieldNameEn;
    private String categoryCode;
    private Integer genderScope;
    private String fieldType;
    private String unit;
    private String unitEn;
    private String refRangeText;
    private String refRangeTextEn;
    private String description;
    private Integer isRequired;
    private String enumOptions;
    private Integer sortOrder;
    private String remark;
    private String remarkEn;
    private String aliasName;
    private String aliasNameEn;
}
