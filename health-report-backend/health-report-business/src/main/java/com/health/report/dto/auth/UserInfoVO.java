package com.health.report.dto.auth;

import lombok.Data;

import java.util.List;

/** 前端 user-info 接口返回结构 */
@Data
public class UserInfoVO {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
}
