package com.backend.cv_scanner.model.mapper;

import com.backend.cv_scanner.dao.entity.CVBatch;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.model.dto.batch.CVBatchResponseDto;
import com.backend.cv_scanner.model.dto.batch.CVFileResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CVBatchMapper {

    CVBatchResponseDto toDto(CVBatch batch);

    CVFileResponseDto toDto(CVFile file);
}
