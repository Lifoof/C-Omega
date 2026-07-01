package com.health.report.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeRequest {
    @NotBlank
    private String target;   // 手机号或邮箱
    private Integer type;   // 1注册 2登录 3找回密码 -> 用 1 表示发送验证码
}
