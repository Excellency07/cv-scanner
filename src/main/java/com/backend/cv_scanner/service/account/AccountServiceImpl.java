package com.backend.cv_scanner.service.account;

import com.backend.cv_scanner.dao.entity.Account;
import com.backend.cv_scanner.dao.repository.jpa.AccountRepository;
import com.backend.cv_scanner.exception.AppAccountNotFoundException;
import com.backend.cv_scanner.exception.EmailAlreadyExistsException;
import com.backend.cv_scanner.exception.ForbiddenOperationException;
import com.backend.cv_scanner.exception.InvalidPasswordException;
import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.account.AccountResponseDto;
import com.backend.cv_scanner.model.dto.account.ResetPasswordRequestDto;
import com.backend.cv_scanner.model.dto.account.UpdateAccountRequestDto;
import com.backend.cv_scanner.model.enums.AccountRole;
import com.backend.cv_scanner.model.mapper.AccountMapper;
import com.backend.cv_scanner.specification.AccountSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper accountMapper;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public PageResponseDto<AccountResponseDto> search(Pageable pageable, String keyword, Instant dateFrom, Instant dateTo) {
        Specification<Account> specification = AccountSpecification.getAccountSpecification(keyword, dateFrom, dateTo);
        Page<Account> accountPage = accountRepository.findAll(specification, pageable);
        return PageResponseDto.toDto(accountPage.map(accountMapper::toDto));
    }

    @Transactional(readOnly = true)
    @Override
    public AccountResponseDto getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow( () -> new AppAccountNotFoundException("Account not found"));

        return accountMapper.toDto(account);
    }

    @Transactional
    @Override
    public AccountResponseDto updateAccount(UUID accountId, UpdateAccountRequestDto requestDto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow( () -> new AppAccountNotFoundException("Account not found"));

        String updatedName = requestDto.name();
        String updatedEmail = requestDto.email();

        if (updatedName != null) {
            account.setName(updatedName);
        }

        if (updatedEmail != null) {
            if (accountRepository.existsByEmail(updatedEmail)) throw new EmailAlreadyExistsException("Email already exists");
            account.setEmail(updatedEmail);
        }

        accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Transactional
    @Override
    public void resetPassword(UUID accountId, ResetPasswordRequestDto requestDto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppAccountNotFoundException("Account not found"));

        if (!passwordEncoder.matches(requestDto.currentPassword(), account.getPassword())) throw new InvalidPasswordException("Current password is not valid");
        account.setPassword(passwordEncoder.encode(requestDto.newPassword()));
        accountRepository.save(account);
    }

    @Transactional
    @Override
    public void deleteAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppAccountNotFoundException("Account not found"));

        checkIsAdmin(account);
        accountRepository.delete(account);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Override
    public void enable(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow( () -> new AppAccountNotFoundException("Account not found"));

        checkIsAdmin(account);
        account.setEnabled(true);
        accountRepository.save(account);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    @Override
    public void disable(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow( () -> new AppAccountNotFoundException("Account not found"));

        checkIsAdmin(account);
        account.setEnabled(false);
        accountRepository.save(account);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    @Override
    public void addRole(UUID accountId, AccountRole role) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppAccountNotFoundException("Account not found"));

        checkIsAdmin(account);
        account.addRole(role);
        accountRepository.save(account);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    @Override
    public void removeRole(UUID accountId, AccountRole role) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppAccountNotFoundException("Account not found"));

        checkIsAdmin(account);
        account.removeRole(role);
        accountRepository.save(account);
    }

    private void checkIsAdmin(Account account) {
        if (account.getRoles().contains(AccountRole.ADMIN)) throw new  ForbiddenOperationException("Admin account cannot be disabled");
    }

}
