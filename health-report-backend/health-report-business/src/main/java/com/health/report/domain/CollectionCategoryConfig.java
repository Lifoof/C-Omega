package com.health.report.domain;

import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionCategoryConfig extends BaseEntity {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private String categoryNameEn;
    private Integer genderScope;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private Integer deleted;
}
