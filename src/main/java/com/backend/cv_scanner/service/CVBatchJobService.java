package com.backend.cv_scanner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CVBatchJobService {

    private final JobOperator jobOperator;

    @Qualifier("processCvBatchJob")
    private final Job processCvBatchJob;

    public Long startBatch(UUID batchId) {

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("batchId", batchId.toString())
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution =
                    jobOperator.start(
                            processCvBatchJob,
                            jobParameters
                    );

            return jobExecution.getId();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to start CV processing job",
                    e
            );
        }
    }
}