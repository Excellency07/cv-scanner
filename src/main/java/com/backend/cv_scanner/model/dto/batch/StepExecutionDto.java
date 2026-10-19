package com.backend.cv_scanner.model.dto.batch;

import java.time.LocalDateTime;

public record StepExecutionDto(
        String stepName,
        String status,
        long readCount,
        long writeCount,
        long filterCount,
        long readSkipCount,
        long processSkipCount,
        long writeSkipCount,
        long commitCount,
        long rollbackCount,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
