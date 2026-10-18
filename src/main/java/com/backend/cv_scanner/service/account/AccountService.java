package com.backend.cv_scanner.service.account;

import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.account.AccountResponseDto;
import com.backend.cv_scanner.model.dto.account.ResetPasswordRequestDto;
import com.backend.cv_scanner.model.dto.account.UpdateAccountRequestDto;
import com.backend.cv_scanner.model.enums.AccountRole;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AccountService {
    PageResponseDto<AccountResponseDto> search(Pageable pageable, String keyword, Instant dateFrom, Instant dateTo);
    AccountResponseDto getAccountById(UUID accountId);
    AccountResponseDto updateAccount(UUID accountId, UpdateAccountRequestDto requestDto);
    void resetPassword(UUID accountId, ResetPasswordRequestDto requestDto);
    void deleteAccount(UUID accountId);
    void enable(UUID accountId);
    void disable(UUID accountId);
    void addRole(UUID accountId, AccountRole role);
    void removeRole(UUID accountId, AccountRole role);
}
