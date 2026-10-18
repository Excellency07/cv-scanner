package com.backend.cv_scanner.dao.repository.jpa;

import com.backend.cv_scanner.dao.entity.CVFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CVFileRepository extends JpaRepository<CVFile, UUID> {
    Page<CVFile> findAllByBatchId(UUID batchId, Pageable pageable);
}
