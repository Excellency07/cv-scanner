package com.backend.cv_scanner.model.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequestDto(
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 character long")
        String name,

        @Email
        String email
) {
    public UpdateAccountRequestDto {
        name = name != null ? name.trim() : null;
        email = email != null ? email.trim() : null;
    }
}
