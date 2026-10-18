package com.backend.cv_scanner.model.dto;

import java.util.UUID;

public record CVBatchUploadResponse(
        UUID batchId,
        String originalFilename,
        int uploadedFiles,
        int failedFiles,
        String status
) {
}
