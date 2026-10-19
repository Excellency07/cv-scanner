package com.backend.cv_scanner.batch;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CVBatchSkipListener
        implements SkipListener<CVFile, Candidate> {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final CVFileRepository cvFileRepository;

    @Override
    public void onSkipInRead(@NonNull Throwable throwable) {
        log.warn("Skipped unreadable CV record: {}", throwable.getMessage());
    }

    @Override
    public void onSkipInProcess(
            @NonNull CVFile cvFile,
            @NonNull Throwable throwable
    ) {
        markFailed(cvFile, throwable);
    }

    @Override
    public void onSkipInWrite(
            @NonNull Candidate candidate,
            @NonNull Throwable throwable
    ) {
        if (candidate.getFile() != null) {
            markFailed(candidate.getFile(), throwable);
        }
    }

    private void markFailed(CVFile cvFile, Throwable throwable) {

        String reason = describe(throwable);

        log.warn("Skipping CV '{}' ({}): {}", cvFile.getOriginalFilename(), cvFile.getId(), reason);

        cvFile.setStatus(CVFileStatus.FAILED);
        cvFile.setErrorMessage(reason);

        cvFileRepository.save(cvFile);
    }

    private String describe(Throwable throwable) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(throwable);

        String message = throwable == rootCause || rootCause.getMessage() == null
                ? String.valueOf(throwable.getMessage())
                : throwable.getMessage() + " (cause: " + rootCause.getMessage() + ")";

        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
