package com.backend.cv_scanner.model.dto.batch;

import com.backend.cv_scanner.model.enums.CVBatchStatus;

import java.time.Instant;
import java.util.UUID;

public record CVBatchResponseDto(
        UUID id,
        String originalFilename,
        CVBatchStatus status,
        int total,
        int processedFiles,
        int failedFiles,
        String createdBy,
        Long jobExecutionId,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
}
