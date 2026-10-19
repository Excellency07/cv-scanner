package com.backend.cv_scanner.service;

import com.backend.cv_scanner.dao.entity.CVBatch;
import com.backend.cv_scanner.dao.repository.jpa.CVBatchRepository;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.exception.CVBatchNotFoundException;
import com.backend.cv_scanner.exception.InvalidBatchStateException;
import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.batch.*;
import com.backend.cv_scanner.model.enums.CVBatchStatus;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import com.backend.cv_scanner.model.mapper.CVBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CVBatchJobService {

    // a batch can be (re)processed from these states; a re-run only parses files that are not COMPLETED yet
    private static final Set<CVBatchStatus> STARTABLE_STATUSES = Set.of(
            CVBatchStatus.PENDING,
            CVBatchStatus.COMPLETED_WITH_ERRORS,
            CVBatchStatus.FAILED
    );

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final CVBatchRepository cvBatchRepository;
    private final CVFileRepository cvFileRepository;
    private final CVBatchMapper cvBatchMapper;

    @Qualifier("processCvBatchJob")
    private final Job processCvBatchJob;

    public Long startBatch(UUID batchId) {

        CVBatch batch = findBatch(batchId);

        if (!STARTABLE_STATUSES.contains(batch.getStatus())) {
            throw new InvalidBatchStateException(
                    "Batch cannot be processed while it is " + batch.getStatus()
            );
        }

        if (cvFileRepository.countByBatchId(batchId) == 0) {
            throw new InvalidBatchStateException("Batch does not contain any stored CV files");
        }

        CVBatchStatus previousStatus = batch.getStatus();

        // claim the batch before launching so a double click cannot start two jobs
        batch.setStatus(CVBatchStatus.PROCESSING);
        cvBatchRepository.save(batch);

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("batchId", batchId.toString())
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            // runs asynchronously (see BatchConfig#batchTaskExecutor)
            JobExecution jobExecution =
                    jobOperator.start(
                            processCvBatchJob,
                            jobParameters
                    );

            return jobExecution.getId();

        } catch (Exception e) {
            batch.setStatus(previousStatus);
            cvBatchRepository.save(batch);

            throw new IllegalStateException(
                    "Failed to start CV processing job",
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CVBatchResponseDto> getBatches(Pageable pageable) {
        Page<CVBatch> batches = cvBatchRepository.findAll(pageable);
        return PageResponseDto.toDto(batches.map(cvBatchMapper::toDto));
    }

    @Transactional(readOnly = true)
    public CVBatchStatusResponseDto getStatus(UUID batchId) {

        CVBatch batch = findBatch(batchId);

        Map<CVFileStatus, Long> counts = new EnumMap<>(CVFileStatus.class);
        long storedFiles = 0;
        for (CVFileStatus status : CVFileStatus.values()) {
            long count = cvFileRepository.countByBatchIdAndStatus(batchId, status);
            counts.put(status, count);
            storedFiles += count;
        }

        long finishedFiles = counts.get(CVFileStatus.COMPLETED) + counts.get(CVFileStatus.FAILED);
        double progress = storedFiles == 0 ? 0 : Math.round(finishedFiles * 1000.0 / storedFiles) / 10.0;

        JobExecutionDto jobExecution = batch.getJobExecutionId() == null
                ? null
                : toDto(jobRepository.getJobExecution(batch.getJobExecutionId()));

        return new CVBatchStatusResponseDto(
                cvBatchMapper.toDto(batch),
                counts,
                progress,
                jobExecution
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CVFileResponseDto> getFiles(UUID batchId, CVFileStatus status, Pageable pageable) {

        findBatch(batchId);

        var files = status == null
                ? cvFileRepository.findAllByBatchId(batchId, pageable)
                : cvFileRepository.findAllByBatchIdAndStatus(batchId, status, pageable);

        return PageResponseDto.toDto(files.map(cvBatchMapper::toDto));
    }

    private CVBatch findBatch(UUID batchId) {
        return cvBatchRepository.findById(batchId)
                .orElseThrow(() -> new CVBatchNotFoundException("CV batch not found: " + batchId));
    }

    private JobExecutionDto toDto(JobExecution execution) {

        if (execution == null) {
            return null;
        }

        List<StepExecutionDto> steps = execution.getStepExecutions().stream()
                .map(this::toDto)
                .toList();

        List<String> failures = execution.getAllFailureExceptions().stream()
                .map(throwable -> throwable.getClass().getSimpleName() + ": " + throwable.getMessage())
                .toList();

        return new JobExecutionDto(
                execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                execution.getStartTime(),
                execution.getEndTime(),
                steps,
                failures
        );
    }

    private StepExecutionDto toDto(StepExecution step) {
        return new StepExecutionDto(
                step.getStepName(),
                step.getStatus().name(),
                step.getReadCount(),
                step.getWriteCount(),
                step.getFilterCount(),
                step.getReadSkipCount(),
                step.getProcessSkipCount(),
                step.getWriteSkipCount(),
                step.getCommitCount(),
                step.getRollbackCount(),
                step.getStartTime(),
                step.getEndTime()
        );
    }
}
