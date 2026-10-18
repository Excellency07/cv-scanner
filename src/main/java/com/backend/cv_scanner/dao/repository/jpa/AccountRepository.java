package com.backend.cv_scanner.dao.repository.jpa;

import com.backend.cv_scanner.dao.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    boolean existsByEmail(String email);

    @Override
    Optional<Account> findById(UUID id);

    Optional<Account> findByEmail(String email);
}
