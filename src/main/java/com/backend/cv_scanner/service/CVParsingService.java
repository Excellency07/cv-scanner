package com.backend.cv_scanner.service;

import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.dao.repository.jpa.CandidateRepository;
import com.backend.cv_scanner.model.dto.CandidateDetailsDto;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import com.backend.cv_scanner.parser.CVTextExtractor;
import com.backend.cv_scanner.parser.CandidateDetailsExtractor;
import com.backend.cv_scanner.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CVParsingService {

    private final CVFileRepository cvFileRepository;
    private final CandidateRepository candidateRepository;
    private final StorageService storageService;
    private final CVTextExtractor cvTextExtractor;
    private final CandidateDetailsExtractor candidateDetailsExtractor;

    @Transactional
    public Candidate process(UUID fileId) throws IOException {
        CVFile file = cvFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("CV file not found: " + fileId));

        if (candidateRepository.existsByFileId(fileId))
            throw new IllegalStateException("A candidate already exists for CV file: " + fileId);

        try (InputStream inputStream = storageService.download(
                file.getStorageFilename(),
                file.getOriginalFilename()
        )) {
            String text = cvTextExtractor.extractText(
                    inputStream,
                    file.getOriginalFilename()
            );

            CandidateDetailsDto candidateDetails = candidateDetailsExtractor.extract(text);

            Candidate candidate = new Candidate(
                    candidateDetails.fullName(),
                    candidateDetails.email(),
                    candidateDetails.phone(),
                    file
            );

            Candidate savedCandidate = candidateRepository.save(candidate);

            file.setStatus(CVFileStatus.COMPLETED);
            cvFileRepository.save(file);

            return savedCandidate;

        } catch (IOException | RuntimeException e) {
            file.setStatus(CVFileStatus.FAILED);
            cvFileRepository.save(file);
            throw e;
        }
    }

    public Candidate parse(CVFile cvFile) throws IOException {

        try (InputStream inputStream = storageService.download(
                cvFile.getStorageFilename(),
                cvFile.getOriginalFilename()
        )) {
            String text = cvTextExtractor.extractText(
                    inputStream,
                    cvFile.getOriginalFilename()
            );

            com.backend.cv_scanner.model.dto.CandidateDetailsDto candidateDetails = candidateDetailsExtractor.extract(text);

            return new Candidate(
                    candidateDetails.fullName(),
                    candidateDetails.email(),
                    candidateDetails.phone(),
                    cvFile
            );
        }
    }
}