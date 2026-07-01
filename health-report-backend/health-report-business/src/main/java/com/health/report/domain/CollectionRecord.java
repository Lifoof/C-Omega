package com.health.report.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.health.report.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class CollectionRecord extends BaseEntity {

    /** 主键ID */
    private Long id;

    /** 采集编号 */
    private String collectionNo;

    /** 用户ID（当前登录人） */
    private Long userId;

    /** 会员ID */
    private Long memberId;
    /** 会员姓名 */
    private String memberName;

    /** 会员性别 */
    private Integer memberGender;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 体检医院 */
    private String hospital;

    /** 完成率 */
    private BigDecimal completeness;

    /** 来源类型：1手动 2Excel */
    private Integer sourceType;

    /** 报告生成状态：0未生成 1生成中 2已生成 */
    private Integer reportStatus;

    /** 采集完成状态：0草稿 1已完成 */
    private Integer finishStatus;

    /** 删除标志（0代表存在 2代表删除） */
    private Integer deleted;

    private String startDate;
    private String endDate;

}
