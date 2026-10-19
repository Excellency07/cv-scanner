package com.backend.cv_scanner.controller;

import com.backend.cv_scanner.model.dto.CVBatchUploadResponse;
import com.backend.cv_scanner.security.account.AccountDetails;
import com.backend.cv_scanner.service.CVFileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
@Tag(name = "CV Upload")
public class CVFileUploadController {

    private final CVFileUploadService cvFileUploadService;

    @Operation(summary = "Upload a ZIP of PDF/DOCX CVs",
            description = "Unzips and stores the CVs as a new batch. Start parsing with POST /api/cv-batches/{batchId}/process.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CVBatchUploadResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                            @AuthenticationPrincipal AccountDetails accountDetails) {
        CVBatchUploadResponse response = cvFileUploadService.upload(file, accountDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
