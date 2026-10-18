package com.backend.cv_scanner.security.jwt;

import com.backend.cv_scanner.security.account.AccountDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secretKey}")
    private String SECRET_KEY;

    @Getter
    @Value("${jwt.accessTokenExpirationTime}")
    private long EXPIRATION_TIME;

    public String generateToken(AccountDetails accountDetails) {
        Map<String, Object> claims = new HashMap<>();
        Instant currentTime = Instant.now();
        Instant expirationTime = currentTime.plusMillis(EXPIRATION_TIME);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(accountDetails.getUsername())
                .issuedAt(Date.from(currentTime))
                .expiration(Date.from(expirationTime))
                .and()
                .signWith(getSignKey())
                .compact();
    }

    private SecretKey getSignKey() {
        byte[] key = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(key);
    }

    public boolean isTokenNonExpired(String token) {
        // FIXED: Expiration must be in the FUTURE
        return extractExpiration(token).isAfter(Instant.now());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()) && isTokenNonExpired(token));
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return claims != null ? resolver.apply(claims) : null;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // Gracefully return null if token is expired, invalid, or malformed
            return null;
        }
    }
}