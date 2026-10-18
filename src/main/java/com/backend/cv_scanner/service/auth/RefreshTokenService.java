package com.backend.cv_scanner.service.auth;

import com.backend.cv_scanner.dao.entity.RefreshToken;
import com.backend.cv_scanner.model.dto.auth.RefreshTokenRequestDto;
import com.backend.cv_scanner.model.dto.auth.RefreshTokenResponseDto;

import java.util.UUID;

public interface RefreshTokenService {
    RefreshToken generateRefreshToken(UUID accountId);
    void deleteRefreshToken(String refreshToken);
    RefreshTokenResponseDto refreshAccessToken(UUID accountId, RefreshTokenRequestDto requestDto);
}
