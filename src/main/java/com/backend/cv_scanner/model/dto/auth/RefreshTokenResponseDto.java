package com.backend.cv_scanner.model.dto.auth;

public record RefreshTokenResponseDto(
        String accessToken,
        String refreshToken
) {
}
