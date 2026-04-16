package com.shushant.hospital_management.modules.auth.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        AuthUserResponse user) {
}
