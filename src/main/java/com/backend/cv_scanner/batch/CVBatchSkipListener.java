package com.backend.cv_scanner.batch;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CVBatchSkipListener
        implements SkipListener<CVFile, Candidate> {

    private final CVFileRepository cvFileRepository;

    @Override
    public void onSkipInRead(@NonNull Throwable throwable) {
    }

    @Override
    public void onSkipInProcess(
            @NonNull CVFile cvFile,
            @NonNull Throwable throwable
    ) {
        markFailed(cvFile);
    }

    @Override
    public void onSkipInWrite(
            @NonNull Candidate candidate,
            @NonNull Throwable throwable
    ) {
        if (candidate.getFile() != null) {
            markFailed(candidate.getFile());
        }
    }

    private void markFailed(CVFile cvFile) {

        cvFile.setStatus(CVFileStatus.FAILED);

        cvFileRepository.save(cvFile);
    }
}