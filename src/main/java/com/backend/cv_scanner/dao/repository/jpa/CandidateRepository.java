package com.backend.cv_scanner.dao.repository.jpa;

import com.backend.cv_scanner.dao.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID>, JpaSpecificationExecutor<Candidate> {

    boolean existsByFileId(UUID fileId);
}
