package com.health.report.dto.collection;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CollectionDetailVO {
    private CollectionRecordVO record;
    private List<CategoryDataVO> categoryDataList;

    @Data
    public static class CategoryDataVO {
        private String categoryCode;
        private String categoryName;
        private Map<String, Object> fieldData;
        private Integer filledCount;
        private Boolean isCompleted;
    }
}
