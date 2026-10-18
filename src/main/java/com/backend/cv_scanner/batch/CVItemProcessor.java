package com.backend.cv_scanner.batch;

import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import com.backend.cv_scanner.service.CVParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CVItemProcessor implements ItemProcessor<CVFile, Candidate> {

    private final CVParsingService cvParsingService;

    @Override
    public Candidate process(CVFile file) throws Exception {

        file.setStatus(CVFileStatus.PROCESSING);

        return cvParsingService.parse(file);
    }
}