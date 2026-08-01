package com.pebble.mvp.admin;

import com.pebble.mvp.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.mvp.admin.service.AdminStatsService;
import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.matching.service.MatchingService;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CacheIntegrationTest {

    @Autowired
    AdminStatsService adminStatsService;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MatchingService matchingService;
    @Autowired
    CacheManager cacheManager;

    @Test
    void 통계는_캐시되고_매칭_변경_시_무효화된다() {
        cacheManager.getCache("matchStats").clear();

        // 1) 첫 조회 → 캐시 적재
        MatchStatsResponse first = adminStatsService.matchStats();

        // 2) 리포지토리로 직접 매칭 추가 (evict를 우회하는 변경)
        MatchRecord direct = matchRecordRepository.save(MatchRecord.builder()
                .requesterId(2L).partnerId(3L)
                .status(MatchStatus.REQUESTED).score(10.0)
                .createdAt(LocalDateTime.now())
                .build());

        // 3) 캐시 적중 — 직접 변경은 반영되지 않아야 한다
        MatchStatsResponse cached = adminStatsService.matchStats();
        assertThat(cached.totalMatches()).isEqualTo(first.totalMatches());

        // 4) 서비스 경유 매칭 요청 → @CacheEvict → 최신 통계 반영
        User requester = userRepository.findByEmail("male3@match.com").orElseThrow();
        Long partnerId = userRepository.findAll().stream()
                .filter(u -> u.getGender() != null && u.getGender() != requester.getGender())
                .filter(u -> u.getStatus().name().equals("ACTIVE"))
                .filter(u -> !matchRecordRepository.existsByRequesterIdAndPartnerId(requester.getId(), u.getId()))
                .findFirst().orElseThrow().getId();
        Long requested = matchingService.request(requester, partnerId).id();

        MatchStatsResponse refreshed = adminStatsService.matchStats();
        assertThat(refreshed.totalMatches()).isGreaterThanOrEqualTo(first.totalMatches() + 2);

        // 공유 컨텍스트 보호 — 생성 데이터 정리 후 캐시 초기화
        matchRecordRepository.deleteById(direct.getId());
        matchRecordRepository.deleteById(requested);
        cacheManager.getCache("matchStats").clear();
    }
}
