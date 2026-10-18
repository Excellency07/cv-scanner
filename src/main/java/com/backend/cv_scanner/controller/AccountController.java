package com.backend.cv_scanner.controller;

import com.backend.cv_scanner.dao.entity.Account;
import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.account.AccountResponseDto;
import com.backend.cv_scanner.model.dto.account.ResetPasswordRequestDto;
import com.backend.cv_scanner.model.dto.account.UpdateAccountRequestDto;
import com.backend.cv_scanner.security.account.AccountDetails;
import com.backend.cv_scanner.service.account.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/welcome")
    public String welcome(){
        return "Welcome to CV Scanner!";
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<AccountResponseDto>> search(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo
    ) {
        Sort.Direction direction = sortDirection.equals("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, Account::getCreatedAt);
        if (sortBy != null) {
            sort = switch (sortBy) {
                case "name" -> Sort.by(direction, Account::getName);
                case "email" -> Sort.by(direction, Account::getEmail);
                default -> Sort.by(direction, Account::getCreatedAt);
            };
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        PageResponseDto<AccountResponseDto> responseDto = accountService.search(pageable, keyword, dateFrom, dateTo);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponseDto> getAccountById(@PathVariable("accountId") UUID accountId){
        AccountResponseDto responseDto = accountService.getAccountById(accountId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponseDto> getProfile(@AuthenticationPrincipal AccountDetails accountDetails){
        UUID accountId = accountDetails.getId();
        AccountResponseDto responseDto = accountService.getAccountById(accountId);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/me/update")
    public ResponseEntity<AccountResponseDto> updateProfile(@AuthenticationPrincipal AccountDetails accountDetails,
                                                            @Valid @RequestBody UpdateAccountRequestDto requestDto){
        UUID accountId = accountDetails.getId();
        AccountResponseDto responseDto = accountService.updateAccount(accountId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/me/reset-password")
    public ResponseEntity<String> resetPassword(@AuthenticationPrincipal AccountDetails accountDetails,
                                                            @Valid @RequestBody ResetPasswordRequestDto requestDto){
        UUID accountId = accountDetails.getId();
        accountService.resetPassword(accountId, requestDto);
        return ResponseEntity.ok("Reset Password Successful");
    }

    @DeleteMapping("me/delete")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal AccountDetails accountDetails){
        UUID accountId = accountDetails.getId();
        accountService.deleteAccount(accountId);
        return ResponseEntity.ok("Delete Account Successful");
    }

    @PatchMapping("/{accountId}/enable")
    public String enableAccount(@PathVariable UUID accountId){
        accountService.enable(accountId);
        return "Account Enabled";
    }

    @PatchMapping("/{accountId}/disable")
    public String disableAccount(@PathVariable UUID accountId){
        accountService.disable(accountId);
        return "Account Disabled";
    }
}
