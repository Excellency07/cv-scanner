package com.backend.cv_scanner.model.dto.batch;

import com.backend.cv_scanner.model.enums.CVFileStatus;

import java.util.Map;

/**
 * Live view of a batch: file counts are read straight from cv_files, so progress is
 * visible while the job is still running; jobExecution holds Spring Batch's own statistics.
 */
public record CVBatchStatusResponseDto(
        CVBatchResponseDto batch,
        Map<CVFileStatus, Long> fileStatusCounts,
        double progressPercent,
        JobExecutionDto jobExecution
) {
}
