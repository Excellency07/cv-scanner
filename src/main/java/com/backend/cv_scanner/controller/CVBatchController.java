package com.backend.cv_scanner.controller;

import com.backend.cv_scanner.model.dto.CVBatchUploadResponse;
import com.backend.cv_scanner.service.CVBatchJobService;
import com.backend.cv_scanner.service.CVFileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/cv-batches")
@RequiredArgsConstructor
public class CVBatchController {

    private final CVBatchJobService cvBatchJobService;

    @PostMapping("/{batchId}/process")
    public ResponseEntity<ProcessBatchResponse> process(
            @PathVariable UUID batchId
    ) {
        Long jobExecutionId =
                cvBatchJobService.startBatch(batchId);

        return ResponseEntity.ok(
                new ProcessBatchResponse(
                        batchId,
                        jobExecutionId,
                        "CV processing started"
                )
        );
    }

//    @GetMapping("/{batchId}")
//    public ResponseEntity<CVBatchStatusResponse> getStatus(
//            @PathVariable UUID batchId
//    ) {
//        return ResponseEntity.ok(
//                cvBatchJobService.getStatus(batchId)
//        );
//    }

    public record ProcessBatchResponse(
            UUID batchId,
            Long jobExecutionId,
            String message
    ) {}
}