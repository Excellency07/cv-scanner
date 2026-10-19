package com.backend.cv_scanner.model.dto.candidate;

import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CandidateResponseDto(
        UUID id,
        String fullName,
        String email,
        String phone,
        Integer yearsOfExperience,
        Set<String> skills,
        String location,
        JobType jobType,
        JobLocationType jobLocationType,
        UUID batchId,
        String sourceFilename,
        Instant createdAt
) {
}
