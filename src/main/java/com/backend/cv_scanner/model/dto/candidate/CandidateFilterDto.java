package com.backend.cv_scanner.model.dto.candidate;

import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;

import java.util.List;
import java.util.UUID;

/**
 * Query parameters for searching / exporting candidates, e.g.
 * ?skills=Java,Spring Boot&minExperience=5&jobLocationType=REMOTE
 */
public record CandidateFilterDto(
        String keyword,
        List<String> skills,
        Integer minExperience,
        Integer maxExperience,
        String location,
        JobType jobType,
        JobLocationType jobLocationType,
        UUID batchId
) {
}
