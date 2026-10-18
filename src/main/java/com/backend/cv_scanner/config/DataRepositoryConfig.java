package com.backend.cv_scanner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.backend.cv_scanner.dao.repository.redis")
@EnableJpaRepositories(basePackages = "com.backend.cv_scanner.dao.repository.jpa")
public class DataRepositoryConfig {
}
