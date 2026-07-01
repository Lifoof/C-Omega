package com.health.report.dto.collection;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CollectionCreateDTO {
    private Long memberId;
    private LocalDate checkDate;
    private String hospital;
}
