package com.health.report.domain;

import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReportModelRel extends BaseEntity {
    private Long id;
    private Long reportId;
    private Long modelId;
}
