package com.backend.cv_scanner.service.auth;

import com.backend.cv_scanner.dao.entity.Account;
import com.backend.cv_scanner.dao.entity.RefreshToken;
import com.backend.cv_scanner.dao.repository.jpa.AccountRepository;
import com.backend.cv_scanner.dao.repository.redis.RefreshTokenRepository;
import com.backend.cv_scanner.exception.AppAccountNotFoundException;
import com.backend.cv_scanner.exception.TokenNotFoundException;
import com.backend.cv_scanner.model.dto.auth.RefreshTokenRequestDto;
import com.backend.cv_scanner.model.dto.auth.RefreshTokenResponseDto;
import com.backend.cv_scanner.security.account.AccountDetails;
import com.backend.cv_scanner.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtService jwtService;
    @Value("${jwt.refreshTokenExpirationTime}")
    private long EXPIRATION_TIME;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;

    @Override
    public RefreshToken generateRefreshToken(UUID accountId) {
        if (accountId == null || !accountRepository.existsById(accountId)) throw new AppAccountNotFoundException("Account not found");
        String refreshToken = UUID.randomUUID().toString();
        RefreshToken refreshTokenEntity = new RefreshToken(
                refreshToken,
                accountId,
                EXPIRATION_TIME
        );

        return refreshTokenRepository.save(refreshTokenEntity);
    }

    @Override
    public void deleteRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                        .orElseThrow(() -> new TokenNotFoundException("Refresh token not found or expired"));
        refreshTokenRepository.delete(token);
    }

    @Override
    public RefreshTokenResponseDto refreshAccessToken(UUID accountId, RefreshTokenRequestDto requestDto) {
        if (!accountRepository.existsById(accountId)) throw new AppAccountNotFoundException("Account not found");
        if (!refreshTokenRepository.existsById(requestDto.refreshToken())) throw new TokenNotFoundException("Refresh token not found or expired");
        String newRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(
                newRefreshToken,
                accountId,
                EXPIRATION_TIME
        );
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppAccountNotFoundException("Account not found"));

        AccountDetails accountDetails = new AccountDetails(account);
        String accessToken = jwtService.generateToken(accountDetails);
        refreshTokenRepository.save(refreshToken);
        refreshTokenRepository.deleteById(requestDto.refreshToken());
        return new RefreshTokenResponseDto(
                accessToken,
                refreshToken.getToken()
        );
    }
}
