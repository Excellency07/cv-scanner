package com.backend.cv_scanner.batch;

import com.backend.cv_scanner.dao.entity.CVBatch;
import com.backend.cv_scanner.dao.repository.jpa.CVBatchRepository;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.model.enums.CVBatchStatus;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import com.backend.cv_scanner.service.NotificationService;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CVBatchJobListener
        implements JobExecutionListener {

    private final CVBatchRepository cvBatchRepository;
    private final CVFileRepository cvFileRepository;
    private final NotificationService notificationService;

    @Override
    public void beforeJob(@NonNull JobExecution jobExecution) {

        CVBatch batch = getBatch(jobExecution);

        batch.setStatus(CVBatchStatus.PROCESSING);
        batch.setJobExecutionId(jobExecution.getId());
        batch.setStartedAt(Instant.now());
        batch.setFinishedAt(null);

        cvBatchRepository.save(batch);
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {

        CVBatch batch = getBatch(jobExecution);
        UUID batchId = batch.getId();

        long storedFiles = cvFileRepository.countByBatchId(batchId);
        long completedFiles = cvFileRepository.countByBatchIdAndStatus(batchId, CVFileStatus.COMPLETED);
        long parseFailures = cvFileRepository.countByBatchIdAndStatus(batchId, CVFileStatus.FAILED);
        // files that already failed during upload (oversized, storage error) never reached cv_files
        long uploadFailures = Math.max(0, batch.getTotal() - storedFiles);

        batch.setProcessedFiles((int) completedFiles);
        batch.setFailedFiles((int) (parseFailures + uploadFailures));
        batch.setFinishedAt(Instant.now());

        if (jobExecution.getStatus().isUnsuccessful()) {
            batch.setStatus(CVBatchStatus.FAILED);
        } else if (batch.getFailedFiles() > 0) {
            batch.setStatus(CVBatchStatus.COMPLETED_WITH_ERRORS);
        } else {
            batch.setStatus(CVBatchStatus.COMPLETED);
        }

        cvBatchRepository.save(batch);

        notificationService.sendBatchCompleted(batch);
    }

    private CVBatch getBatch(JobExecution jobExecution) {

        UUID batchId = getBatchId(jobExecution);

        return cvBatchRepository.findById(batchId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Batch not found: " + batchId
                        )
                );
    }

    private UUID getBatchId(
            JobExecution jobExecution
    ) {

        String batchId = jobExecution
                .getJobParameters()
                .getString("batchId");

        if (batchId == null) {
            throw new IllegalStateException(
                    "batchId job parameter is missing"
            );
        }

        return UUID.fromString(batchId);
    }
}
