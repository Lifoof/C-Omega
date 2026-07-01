package com.health.report.dto.collection;

import lombok.Data;

@Data
public class CollectionQueryDTO {
    private Long current = 1L;
    private Long size = 10L;
    private String memberName;
    private String startDate;
    private String endDate;
    private Integer reportStatus;
    private Integer status;
}
