package com.backend.cv_scanner.service.auth;

import com.backend.cv_scanner.model.dto.auth.LoginRequestDto;
import com.backend.cv_scanner.model.dto.auth.LoginResponseDto;
import com.backend.cv_scanner.model.dto.auth.RegisterRequestDto;

import java.util.UUID;

public interface AuthService {
    void register(RegisterRequestDto requestDto);
    LoginResponseDto login(LoginRequestDto requestDto);
}
