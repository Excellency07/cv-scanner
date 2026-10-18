package com.backend.cv_scanner.dao.entity;

import com.backend.cv_scanner.model.enums.CVBatchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cv_batches")
public class CVBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    private CVBatchStatus status;

    @Column(nullable = false)
    private int total;

    @Column(name = "processed_files", nullable = false)
    private int processedFiles;

    @Column(name = "failed_files", nullable = false)
    private int failedFiles;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public CVBatch(String originalFilename) {
        this.originalFilename = originalFilename;
    }


}
