package com.health.report.dto.member;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.health.report.common.annotation.Excel;
import lombok.Data;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.Date;

@Data
public class MemberImportInfo {
    private Long id;
    private Long userId;
    private String memberNo;
    @Excel(name = "*姓名", headerBackgroundColor = IndexedColors.RED)
    private String name;
    @Excel(name = "关系",dictType = "member_relationship", comboReadDict = true,headerBackgroundColor = IndexedColors.GREY_50_PERCENT)
    private Integer relationship;
    @Excel(name = "*性别", dictType = "sys_user_sex", comboReadDict = true, headerBackgroundColor = IndexedColors.RED)
    private Integer gender;
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd", prompt = "请输入日期格式：yyyy-MM-dd\n示例：2025-01-01",headerBackgroundColor = IndexedColors.GREY_50_PERCENT)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthDate;
    @Excel(name = "国家", dictType = "country", type = Excel.Type.EXPORT)
    private String country;
    @Excel(name = "城市", type = Excel.Type.EXPORT)
    private String city;
    @Excel(name = "区/县", type = Excel.Type.EXPORT)
    private String district;
    @Excel(name = "联系方式",headerBackgroundColor = IndexedColors.GREY_50_PERCENT)
    private String contact;
    @Excel(name = "地址",headerBackgroundColor = IndexedColors.GREY_50_PERCENT)
    private String address;
    private String ethnicity;
    private String birthplace;
    @Excel(name = "备注",headerBackgroundColor = IndexedColors.GREY_50_PERCENT)
    private String remark;
    private Integer deleted;
}
