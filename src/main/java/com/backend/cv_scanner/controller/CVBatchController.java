package com.backend.cv_scanner.controller;

import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.batch.CVBatchResponseDto;
import com.backend.cv_scanner.model.dto.batch.CVBatchStatusResponseDto;
import com.backend.cv_scanner.model.dto.batch.CVFileResponseDto;
import com.backend.cv_scanner.model.dto.batch.ProcessBatchResponseDto;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import com.backend.cv_scanner.service.CVBatchJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cv-batches")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
@Tag(name = "CV Batches", description = "Start and monitor batch processing")
public class CVBatchController {

    private final CVBatchJobService cvBatchJobService;

    @Operation(summary = "Start (or re-run) parsing of an uploaded batch",
            description = "Runs in the background. A re-run only parses files that have not completed yet.")
    @PostMapping("/{batchId}/process")
    public ResponseEntity<ProcessBatchResponseDto> process(
            @PathVariable UUID batchId
    ) {
        Long jobExecutionId =
                cvBatchJobService.startBatch(batchId);

        return ResponseEntity.accepted().body(
                new ProcessBatchResponseDto(
                        batchId,
                        jobExecutionId,
                        "CV processing started"
                )
        );
    }

    @Operation(summary = "List batches, newest first")
    @GetMapping
    public ResponseEntity<PageResponseDto<CVBatchResponseDto>> getBatches(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize
    ) {
        PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(cvBatchJobService.getBatches(pageable));
    }

    @Operation(summary = "Batch status, progress and Spring Batch step statistics")
    @GetMapping("/{batchId}")
    public ResponseEntity<CVBatchStatusResponseDto> getStatus(
            @PathVariable UUID batchId
    ) {
        return ResponseEntity.ok(
                cvBatchJobService.getStatus(batchId)
        );
    }

    @Operation(summary = "Files of a batch, e.g. ?status=FAILED to see what was skipped and why")
    @GetMapping("/{batchId}/files")
    public ResponseEntity<PageResponseDto<CVFileResponseDto>> getFiles(
            @PathVariable UUID batchId,
            @RequestParam(required = false) CVFileStatus status,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "20") int pageSize
    ) {
        PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt", "id"));
        return ResponseEntity.ok(cvBatchJobService.getFiles(batchId, status, pageable));
    }
}
