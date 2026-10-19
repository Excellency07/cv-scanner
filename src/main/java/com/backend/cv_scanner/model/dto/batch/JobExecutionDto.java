package com.backend.cv_scanner.model.dto.batch;

import java.time.LocalDateTime;
import java.util.List;

public record JobExecutionDto(
        long id,
        String jobName,
        String status,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<StepExecutionDto> steps,
        List<String> failures
) {
}
