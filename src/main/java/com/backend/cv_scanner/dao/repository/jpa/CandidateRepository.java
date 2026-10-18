package com.backend.cv_scanner.dao.repository.jpa;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    boolean existsByFileId(UUID fileId);
}
