package com.backend.cv_scanner.config;

import com.backend.cv_scanner.batch.CVItemProcessor;
import com.backend.cv_scanner.batch.CVItemWriter;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.entity.Candidate;

import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class BatchConfig {

    @Bean
    public Step processCvFilesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("cvItemReader")
            ItemReader<CVFile> reader,
            CVItemProcessor processor,
            CVItemWriter writer
    ) {
        return new StepBuilder("processCvFilesStep", jobRepository)
                .<CVFile, Candidate>chunk(10)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job processCvBatchJob(
            JobRepository jobRepository,
            Step processCvFilesStep
    ) {
        return new JobBuilder("processCvBatchJob", jobRepository)
                .start(processCvFilesStep)
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<CVFile> cvItemReader(
            CVFileRepository cvFileRepository,
            @Value("#{jobParameters['batchId']}") String batchId
    ) {
        return new RepositoryItemReaderBuilder<CVFile>()
                .name("cvFileReader")
                .repository(cvFileRepository)
                .methodName("findAllByBatch_Id")
                .arguments(List.of(UUID.fromString(batchId)))
                .pageSize(10)
                .sorts(Map.of(
                        "createdAt", Sort.Direction.ASC,
                        "id", Sort.Direction.ASC
                ))
                .build();
    }
}