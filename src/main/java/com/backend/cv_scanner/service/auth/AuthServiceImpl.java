package com.backend.cv_scanner.service.auth;

import com.backend.cv_scanner.dao.entity.Account;
import com.backend.cv_scanner.dao.entity.RefreshToken;
import com.backend.cv_scanner.dao.repository.jpa.AccountRepository;
import com.backend.cv_scanner.exception.AppAccountNotFoundException;
import com.backend.cv_scanner.model.dto.auth.LoginRequestDto;
import com.backend.cv_scanner.model.dto.auth.LoginResponseDto;
import com.backend.cv_scanner.model.dto.auth.RegisterRequestDto;
import com.backend.cv_scanner.model.enums.AccountRole;
import com.backend.cv_scanner.security.account.AccountDetails;
import com.backend.cv_scanner.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final AccountRepository accountRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    @Override
    public void register(RegisterRequestDto requestDto) {
        Account account = new Account(
                requestDto.name(),
                requestDto.email(),
                passwordEncoder.encode(requestDto.password())
        );
        account.addRole(AccountRole.DEFAULT);
        accountRepository.save(account);
    }

    @Transactional
    @Override
    public LoginResponseDto login(LoginRequestDto requestDto) {
        Authentication authentication =  authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        AccountDetails accountDetails = (AccountDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(accountDetails);
        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(accountDetails.getId());
        return new LoginResponseDto(
                accessToken,
                refreshToken.getToken(),
                accountDetails.getUsername(),
                jwtService.getEXPIRATION_TIME()
        );
    }
}
