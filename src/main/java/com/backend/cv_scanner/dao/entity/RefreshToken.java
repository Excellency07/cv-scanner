package com.backend.cv_scanner.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("refresh_tokens")
public class RefreshToken {
    @Id
    private String token;

    private UUID accountId;

    @TimeToLive(unit = TimeUnit.MINUTES)
    private long expiresIn;
}
