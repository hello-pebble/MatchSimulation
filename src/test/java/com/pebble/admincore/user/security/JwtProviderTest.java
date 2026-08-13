package com.pebble.admincore.user.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";

    @Test
    void 발급한_토큰에서_userId를_복원한다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);
        String token = provider.issue(42L, "USER");
        assertThat(provider.parseUserId(token)).contains(42L);
    }

    @Test
    void 만료된_토큰은_거부한다() throws InterruptedException {
        JwtProvider provider = new JwtProvider(SECRET, -1); // 발급 시점에 이미 만료
        String token = provider.issue(42L, "USER");
        Thread.sleep(5);
        assertThat(provider.parseUserId(token)).isEmpty();
    }

    @Test
    void 다른_키로_서명된_토큰은_거부한다() {
        JwtProvider attacker = new JwtProvider("attacker-secret-key-also-32-bytes-long!!!!", 60);
        String forged = attacker.issue(1L, "ADMIN");
        JwtProvider provider = new JwtProvider(SECRET, 60);
        assertThat(provider.parseUserId(forged)).isEmpty();
    }

    @Test
    void 변조된_토큰과_빈_토큰은_거부한다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);
        String token = provider.issue(42L, "USER");
        assertThat(provider.parseUserId(token + "x")).isEmpty();
        assertThat(provider.parseUserId(null)).isEmpty();
        assertThat(provider.parseUserId("")).isEmpty();
        assertThat(provider.parseUserId("not-a-jwt")).isEmpty();
    }
}
