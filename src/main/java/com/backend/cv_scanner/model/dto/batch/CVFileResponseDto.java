package com.backend.cv_scanner.model.dto.batch;

import com.backend.cv_scanner.model.enums.CVFileStatus;

import java.time.Instant;
import java.util.UUID;

public record CVFileResponseDto(
        UUID id,
        String originalFilename,
        long fileSize,
        CVFileStatus status,
        String errorMessage,
        Instant createdAt
) {
}
