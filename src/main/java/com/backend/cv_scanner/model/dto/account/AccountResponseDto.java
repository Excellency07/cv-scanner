package com.backend.cv_scanner.model.dto.account;

import com.backend.cv_scanner.model.enums.AccountRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountResponseDto(
        UUID id,
        String name,
        String email,
        List<AccountRole> roles,
        boolean enabled,
        Instant createdAt
) {
}
