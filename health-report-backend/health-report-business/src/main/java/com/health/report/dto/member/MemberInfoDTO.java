package com.health.report.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberInfoDTO {
    private Long id;
    @NotNull(message = "与本人关系不能为空")
    private Integer relationship;
    @NotBlank(message = "姓名不能为空")
    private String name;
    private Integer gender;
    @JsonProperty("dateOfBirth")
    private LocalDate birthDate;
    private String country;
    private String city;
    private String district;
    private String contact;
    private String address;
    private String ethnicity;
    private String birthplace;
    private String remark;
}
