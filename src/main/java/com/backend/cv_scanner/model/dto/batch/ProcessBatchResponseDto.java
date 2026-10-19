package com.backend.cv_scanner.model.dto.batch;

import java.util.UUID;

public record ProcessBatchResponseDto(
        UUID batchId,
        Long jobExecutionId,
        String message
) {
}
