package com.backend.cv_scanner.model.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 character long")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$",
                message = "Password must be at least 6 characters long and include an uppercase letter, lowercase letter, number, and special character")
        String newPassword
) {
    public ResetPasswordRequestDto {
        currentPassword = currentPassword != null ? currentPassword.trim() : null;
        newPassword = newPassword != null ? newPassword.trim() : null;
    }
}