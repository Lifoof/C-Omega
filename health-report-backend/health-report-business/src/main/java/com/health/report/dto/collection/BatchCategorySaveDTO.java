package com.health.report.dto.collection;

import lombok.Data;

import java.util.List;

@Data
public class BatchCategorySaveDTO {
    private Long collectionId;
    private List<CategorySaveDTO> categoryList;
}
