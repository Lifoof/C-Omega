package com.health.report.domain;


import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class Report extends BaseEntity {

    private Long id;
    private Long userId;
    private Long memberId;
    private Long collectionId;
    private Long modelId;
    private String reportNo;
    private String memberName;
    private String modelNameEn;
    private String modelName;
    private BigDecimal completeness;
    private Integer status;
    private String reportContent;
    private String filePath;
    private String filePathEn;
    private Date generationTime;
    private Integer deleted;

    private String startDate;
    private String endDate;
}
