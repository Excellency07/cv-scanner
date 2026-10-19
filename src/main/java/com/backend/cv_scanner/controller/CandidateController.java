package com.backend.cv_scanner.controller;

import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.candidate.CandidateFilterDto;
import com.backend.cv_scanner.model.dto.candidate.CandidateResponseDto;
import com.backend.cv_scanner.model.enums.ExportFormat;
import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;
import com.backend.cv_scanner.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
@Tag(name = "Candidates", description = "Search, filter and export parsed candidates")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Search candidates",
            description = "Example: ?skills=Java&minExperience=5&jobLocationType=REMOTE. Several skills must all match.")
    @GetMapping
    public ResponseEntity<PageResponseDto<CandidateResponseDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Integer maxExperience,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) JobLocationType jobLocationType,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection
    ) {
        CandidateFilterDto filter = new CandidateFilterDto(
                keyword, skills, minExperience, maxExperience, location, jobType, jobLocationType, batchId
        );

        Sort.Direction direction = sortDirection.equals("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = switch (sortBy == null ? "" : sortBy) {
            case "name" -> "fullName";
            case "experience" -> "yearsOfExperience";
            default -> "createdAt";
        };

        PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortProperty));
        return ResponseEntity.ok(candidateService.search(filter, pageable));
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<CandidateResponseDto> getById(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(candidateService.getById(candidateId));
    }

    @Operation(summary = "Download matching candidates as CSV or Excel",
            description = "Accepts the same filters as the search endpoint, e.g. ?format=XLSX&skills=Java")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false, defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Integer maxExperience,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) JobLocationType jobLocationType,
            @RequestParam(required = false) UUID batchId,
            HttpServletResponse response
    ) throws IOException {
        CandidateFilterDto filter = new CandidateFilterDto(
                keyword, skills, minExperience, maxExperience, location, jobType, jobLocationType, batchId
        );

        String filename = "candidates-" + LocalDate.now() + "." + format.getExtension();

        response.setContentType(format.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString());

        candidateService.export(filter, format, response.getOutputStream());
    }
}
