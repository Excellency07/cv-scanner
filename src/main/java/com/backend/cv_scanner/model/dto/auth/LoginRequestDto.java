package com.backend.cv_scanner.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
        public LoginRequestDto {
                email = email != null ? email.trim() : null;
                password = password != null ? password.trim() : null;
        }
}
