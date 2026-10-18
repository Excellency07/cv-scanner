package com.backend.cv_scanner.model.dto.auth;

import com.backend.cv_scanner.model.enums.AccountRole;

import java.util.List;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        String type,
        String email,
        long expiresIn
) {
    public LoginResponseDto(String accessToken, String refreshToken, String email, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", email, expiresIn);
    }
}
