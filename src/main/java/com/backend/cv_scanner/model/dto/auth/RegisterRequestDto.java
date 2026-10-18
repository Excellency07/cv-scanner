package com.backend.cv_scanner.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 50 character long")
        String name,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must contain at least 6 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$",
                message = "Password must contain at least one uppercase and one lowercase letters, one number and one symbol")
        String password
) {
        public RegisterRequestDto {
                name = name != null ? name.trim() : null;
                email = email != null ? email.trim() : null;
                password = password != null ? password.trim() : null;
        }
}
