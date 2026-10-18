package com.backend.cv_scanner.batch;

import com.backend.cv_scanner.dao.entity.CVBatch;
import com.backend.cv_scanner.dao.repository.jpa.CVBatchRepository;
import com.backend.cv_scanner.model.enums.CVBatchStatus;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CVBatchJobListener
        implements JobExecutionListener {

    private final CVBatchRepository cvBatchRepository;

    @Override
    public void beforeJob(@NonNull JobExecution jobExecution) {

        UUID batchId = getBatchId(jobExecution);

        CVBatch batch = cvBatchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found: " + batchId
                        )
                );

        batch.setStatus(CVBatchStatus.PROCESSING);

        cvBatchRepository.save(batch);
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {

        UUID batchId = getBatchId(jobExecution);

        CVBatch batch = cvBatchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found: " + batchId
                        )
                );

        if (jobExecution.getStatus().isUnsuccessful()) {
            batch.setStatus(CVBatchStatus.FAILED);
        } else {
            batch.setStatus(CVBatchStatus.COMPLETED);
        }

        cvBatchRepository.save(batch);
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