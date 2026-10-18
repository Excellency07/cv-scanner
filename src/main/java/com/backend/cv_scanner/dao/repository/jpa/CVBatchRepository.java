package com.backend.cv_scanner.dao.repository.jpa;

import com.backend.cv_scanner.dao.entity.CVBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CVBatchRepository extends JpaRepository<CVBatch, UUID> {
}
