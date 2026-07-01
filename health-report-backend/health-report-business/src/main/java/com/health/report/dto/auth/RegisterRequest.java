package com.health.report.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String account;   // 手机号或邮箱
    @NotBlank
    private String password;
    @NotBlank
    private String code;      // 验证码
    private Integer registerType; // 1手机 2邮箱
    private String countryCode = "CN";
    private String nickname;   // 前端 name
}
