package com.pebble.admincore.admin;

import com.pebble.admincore.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.admincore.admin.service.AdminStatsService;
import com.pebble.admincore.matching.domain.MatchRecord;
import com.pebble.admincore.matching.domain.MatchStatus;
import com.pebble.admincore.matching.repository.MatchRecordRepository;
import com.pebble.admincore.matching.service.MatchExpiryScheduler;
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
    MatchExpiryScheduler matchExpiryScheduler;
    @Autowired
    CacheManager cacheManager;

    @Test
    void 통계는_캐시되고_매칭_상태_변경_시_무효화된다() {
        cacheManager.getCache("matchStats").clear();

        // 1) 첫 조회 → 캐시 적재
        MatchStatsResponse first = adminStatsService.matchStats();

        // 2) 리포지토리로 직접 매칭 추가 (evict를 우회하는 변경) — 7일 경과 REQUESTED
        MatchRecord direct = matchRecordRepository.save(MatchRecord.builder()
                .requesterId(2L).partnerId(3L)
                .status(MatchStatus.REQUESTED).score(10.0)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build());

        // 3) 캐시 적중 — 직접 변경은 반영되지 않아야 한다
        MatchStatsResponse cached = adminStatsService.matchStats();
        assertThat(cached.totalMatches()).isEqualTo(first.totalMatches());

        // 4) 만료 배치(관리자 측 유일한 쓰기 경로) → @CacheEvict → 최신 통계 반영
        matchExpiryScheduler.expireStaleRequests();

        MatchStatsResponse refreshed = adminStatsService.matchStats();
        assertThat(refreshed.totalMatches()).isEqualTo(first.totalMatches() + 1);
        assertThat(refreshed.byStatus().get(MatchStatus.EXPIRED.name()))
                .isGreaterThanOrEqualTo(1L);

        // 공유 컨텍스트 보호 — 생성 데이터 정리 후 캐시 초기화
        matchRecordRepository.deleteById(direct.getId());
        cacheManager.getCache("matchStats").clear();
    }
}
