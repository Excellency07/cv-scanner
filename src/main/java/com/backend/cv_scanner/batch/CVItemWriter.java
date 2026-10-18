package com.backend.cv_scanner.batch;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.repository.jpa.CandidateRepository;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CVItemWriter implements ItemWriter<Candidate> {

    private final CandidateRepository candidateRepository;
    private final CVFileRepository cvFileRepository;

    public CVItemWriter(
            CandidateRepository candidateRepository,
            CVFileRepository cvFileRepository
    ) {
        this.candidateRepository = candidateRepository;
        this.cvFileRepository = cvFileRepository;
    }

    @Override
    public void write(Chunk<? extends Candidate> chunk) {

        List<? extends Candidate> candidates = chunk.getItems();

        candidateRepository.saveAll(candidates);

        List<CVFile> processedFiles = candidates.stream()
                .map(Candidate::getFile)
                .peek(file -> file.setStatus(CVFileStatus.COMPLETED))
                .toList();

        cvFileRepository.saveAll(processedFiles);
    }
}