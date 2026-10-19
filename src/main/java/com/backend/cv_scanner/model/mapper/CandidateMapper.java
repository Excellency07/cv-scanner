package com.backend.cv_scanner.model.mapper;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.model.dto.candidate.CandidateResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

    @Mapping(target = "batchId", source = "file.batch.id")
    @Mapping(target = "sourceFilename", source = "file.originalFilename")
    CandidateResponseDto toDto(Candidate candidate);
}
