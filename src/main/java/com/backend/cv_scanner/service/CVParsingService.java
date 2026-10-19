package com.backend.cv_scanner.service;

import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.model.dto.CandidateDetailsDto;
import com.backend.cv_scanner.parser.CVTextExtractor;
import com.backend.cv_scanner.parser.CandidateDetailsExtractor;
import com.backend.cv_scanner.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class CVParsingService {

    private final StorageService storageService;
    private final CVTextExtractor cvTextExtractor;
    private final CandidateDetailsExtractor candidateDetailsExtractor;

    /**
     * Downloads the CV, extracts its text and builds an (unsaved) candidate.
     * Saving is left to the batch writer so it happens inside the chunk transaction.
     */
    public Candidate parse(CVFile cvFile) throws IOException {

        try (InputStream inputStream = storageService.download(
                cvFile.getStorageFilename(),
                cvFile.getOriginalFilename()
        )) {
            String text = cvTextExtractor.extractText(
                    inputStream,
                    cvFile.getOriginalFilename()
            );

            CandidateDetailsDto candidateDetails = candidateDetailsExtractor.extract(text);

            Candidate candidate = new Candidate(
                    candidateDetails.fullName(),
                    candidateDetails.email(),
                    candidateDetails.phone(),
                    cvFile
            );
            candidate.setYearsOfExperience(candidateDetails.yearsOfExperience());
            candidate.setSkills(new LinkedHashSet<>(candidateDetails.skills()));
            candidate.setLocation(candidateDetails.location());
            candidate.setJobType(candidateDetails.jobType());
            candidate.setJobLocationType(candidateDetails.jobLocationType());

            return candidate;
        }
    }
}
