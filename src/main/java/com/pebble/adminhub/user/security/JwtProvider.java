package com.pebble.adminhub.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 발급/검증 (HS256). 무상태 인증 — 서버 재시작에도 토큰이 유효하다.
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration expiry;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    public String issue(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiry.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 서명·만료가 유효하면 userId를 반환, 아니면 empty */
    public Optional<Long> parseUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
