package com.health.report.dto.auth;

import lombok.Data;

@Data
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;   // 秒
    private UserInfoVO userInfo;

    @Data
    public static class UserInfoVO {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private String roleCode;
    }
}
