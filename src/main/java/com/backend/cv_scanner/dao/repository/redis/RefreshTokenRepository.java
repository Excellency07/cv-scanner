package com.backend.cv_scanner.dao.repository.redis;

import com.backend.cv_scanner.dao.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
