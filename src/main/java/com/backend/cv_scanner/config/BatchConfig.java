package com.backend.cv_scanner.config;

import com.backend.cv_scanner.batch.CVBatchJobListener;
import com.backend.cv_scanner.batch.CVBatchSkipListener;
import com.backend.cv_scanner.batch.CVFileSkipPolicy;
import com.backend.cv_scanner.batch.CVItemProcessor;
import com.backend.cv_scanner.batch.CVItemWriter;
import com.backend.cv_scanner.batch.CVRetryListener;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.entity.Candidate;

import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.exception.CVParsingException;
import com.backend.cv_scanner.exception.StorageException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class BatchConfig {

    private static final int CHUNK_SIZE = 10;

    /**
     * Launches jobs in the background so POST /process returns immediately
     * instead of blocking the HTTP request until thousands of CVs are parsed.
     */
    @Bean
    @BatchTaskExecutor
    public TaskExecutor batchTaskExecutor(
            @Value("${app.batch.max-concurrent-jobs:2}") int maxConcurrentJobs
    ) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("cv-batch-");
        executor.setConcurrencyLimit(maxConcurrentJobs);
        return executor;
    }

    /**
     * Temporary problems (MinIO hiccup, network, DB deadlock) are retried with backoff.
     * Unparseable documents (CVParsingException) are not retried: they are skipped right away.
     * The predicate is needed because includes() also matches causes, and Tika/PDFBox wrap an
     * IOException inside the parsing error of a corrupted PDF.
     */
    @Bean
    public RetryPolicy cvRetryPolicy(
            @Value("${app.batch.retry.max-attempts:3}") long maxRetries,
            @Value("${app.batch.retry.delay:500ms}") Duration delay
    ) {
        return RetryPolicy.builder()
                .includes(StorageException.class, IOException.class, TransientDataAccessException.class)
                .predicate(throwable -> !(throwable instanceof CVParsingException))
                .maxRetries(maxRetries)
                .delay(delay)
                .multiplier(2)
                .build();
    }

    @Bean
    public Step processCvFilesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("cvItemReader")
            ItemReader<CVFile> reader,
            CVItemProcessor processor,
            CVItemWriter writer,
            RetryPolicy cvRetryPolicy,
            CVRetryListener retryListener,
            CVFileSkipPolicy skipPolicy,
            CVBatchSkipListener skipListener
    ) {
        return new StepBuilder("processCvFilesStep", jobRepository)
                .<CVFile, Candidate>chunk(CHUNK_SIZE)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .faultTolerant()
                .retryPolicy(cvRetryPolicy)
                .retryListener(retryListener)
                .skipPolicy(skipPolicy)
                .skipListener(skipListener)
                .build();
    }

    @Bean
    public Job processCvBatchJob(
            JobRepository jobRepository,
            Step processCvFilesStep,
            CVBatchJobListener jobListener
    ) {
        return new JobBuilder("processCvBatchJob", jobRepository)
                .listener(jobListener)
                .start(processCvFilesStep)
                .build();
    }

    /**
     * Reads every file of the batch (not only PENDING ones): filtering on a status that the
     * writer changes would shift the pages and silently skip files. Already completed files
     * are filtered out by the processor instead.
     */
    @Bean
    @StepScope
    public RepositoryItemReader<CVFile> cvItemReader(
            CVFileRepository cvFileRepository,
            @Value("#{jobParameters['batchId']}") String batchId
    ) {
        Map<String, Sort.Direction> sorts = new LinkedHashMap<>();
        sorts.put("createdAt", Sort.Direction.ASC);
        sorts.put("id", Sort.Direction.ASC);

        return new RepositoryItemReaderBuilder<CVFile>()
                .name("cvFileReader")
                .repository(cvFileRepository)
                .methodName("findAllByBatchId")
                .arguments(List.of(UUID.fromString(batchId)))
                .pageSize(CHUNK_SIZE)
                .sorts(sorts)
                .build();
    }
}
