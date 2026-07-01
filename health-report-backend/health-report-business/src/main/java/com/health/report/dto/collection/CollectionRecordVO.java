package com.health.report.dto.collection;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class CollectionRecordVO {
    private Long id;
    private String collectionNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;
    private String memberName;
    private Integer memberGender;
    private LocalDate checkDate;
    private String hospital;
    private Integer sourceType;
    private Integer totalFields;
    private Integer filledFields;
    private BigDecimal completeness;
    private Integer reportStatus;
    private Integer status;
    private Date createdTime;
    private Date updatedTime;
}
