package com.backend.cv_scanner.controller;

import com.backend.cv_scanner.model.dto.auth.*;
import com.backend.cv_scanner.security.account.AccountDetails;
import com.backend.cv_scanner.service.auth.AuthService;
import com.backend.cv_scanner.service.auth.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @GetMapping
    public String home(){
        return "Hello World!";
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequestDto requestDto){
        authService.register(requestDto);
        return "Register Success";
    }

    @PostMapping("/login")
    public LoginResponseDto register(@Valid @RequestBody LoginRequestDto requestDto){
        return authService.login(requestDto);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDto> refreshAccessToken(@AuthenticationPrincipal AccountDetails accountDetails,
                                                                      @RequestBody RefreshTokenRequestDto requestDto){
        UUID accountId = accountDetails.getId();
        RefreshTokenResponseDto responseDto = refreshTokenService.refreshAccessToken(accountId, requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
