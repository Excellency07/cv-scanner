package com.backend.cv_scanner.model.dto;

import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;

import java.util.Set;

public record CandidateDetailsDto(
        String fullName,
        String email,
        String phone,
        Integer yearsOfExperience,
        Set<String> skills,
        String location,
        JobType jobType,
        JobLocationType jobLocationType
) {
    public static CandidateDetailsDto empty() {
        return new CandidateDetailsDto(null, null, null, null, Set.of(), null, null, null);
    }
}
