package com.pebble.mvp.user.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory 더미 토큰 저장소 (token → userId).
 * 추후 JWT/Spring Security로 교체 시 이 클래스만 대체하면 된다.
 */
@Component
public class TokenStore {

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, userId);
        return token;
    }

    public Optional<Long> resolve(String token) {
        return Optional.ofNullable(token).map(tokens::get);
    }

    public void revoke(String token) {
        tokens.remove(token);
    }
}
