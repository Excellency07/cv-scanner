package com.backend.cv_scanner.dao.entity;

import com.backend.cv_scanner.model.enums.CVFileStatus;
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
@Table(name = "cv_files")
public class CVFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private CVBatch batch;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_filename", nullable = false, unique = true)
    private String storageFilename;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CVFileStatus status;

    @OneToOne(mappedBy = "file")
    private Candidate candidate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public CVFile(CVBatch batch, String originalFilename, String storageFilename, long fileSize) {
        this.batch = batch;
        this.originalFilename = originalFilename;
        this.storageFilename = storageFilename;
        this.fileSize = fileSize;
    }
}
