package com.health.report.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.health.report.common.annotation.Excel;
import com.health.report.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class MemberInfo extends BaseEntity {

    private Long id;
    private Long userId;
    private String memberNo;
    @Excel(name = "姓名")
    private String name;
    @Excel(name = "关系",dictType = "member_relationship")
    private Integer relationship;
    @Excel(name = "性别", dictType = "sys_user_sex")
    private Integer gender;
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthDate;
    @Excel(name = "国家", dictType = "country", type = Excel.Type.EXPORT)
    private String country;
    private String city;
    private String district;
    @Excel(name = "联系方式")
    private String contact;
    private String address;
    private String ethnicity;
    private String birthplace;
    private String remark;
    private Integer deleted;
    private String startDate;
    private String endDate;
}
