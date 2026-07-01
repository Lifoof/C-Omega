package com.health.report.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank
    private String account;
    @NotBlank
    private String code;
    @NotBlank
    private String newPassword;
}
