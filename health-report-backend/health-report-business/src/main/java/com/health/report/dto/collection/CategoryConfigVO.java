package com.health.report.dto.collection;

import lombok.Data;

import java.util.List;

@Data
public class CategoryConfigVO {
    private Long id;
    private String categoryCode;
    private String categoryName;
    private String categoryNameEn;
    private Integer genderScope;
    private String icon;
    private Integer sortOrder;
    private List<FieldConfigVO> fields;
}
