package com.pebble.adminhub.admin;

import com.pebble.adminhub.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.adminhub.admin.service.AdminStatsService;
import com.pebble.adminhub.matching.repository.MatchRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB GROUP BY 집계가 기존 애플리케이션 집계와 동일한 결과를 내는지 검증한다.
 * 기대값은 JdbcTemplate으로 독립 계산해 서비스 결과와 대조한다.
 */
@SpringBootTest
class AdminStatsAggregationTest {

    @Autowired
    AdminStatsService adminStatsService;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    CacheManager cacheManager;

    @Test
    void 상태별_집계_합계가_전체_건수와_일치한다() {
        cacheManager.getCache("matchStats").clear();
        MatchStatsResponse stats = adminStatsService.matchStats();

        long total = matchRecordRepository.count();
        assertThat(stats.totalMatches()).isEqualTo(total);
        assertThat(stats.byStatus().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(total);
        assertThat(stats.byGender().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(total);
        assertThat(stats.daily().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(total);
    }

    @Test
    void 성사율은_ACCEPTED_비율과_일치한다() {
        cacheManager.getCache("matchStats").clear();
        MatchStatsResponse stats = adminStatsService.matchStats();

        Long accepted = jdbcTemplate.queryForObject(
                "select count(*) from match_records where status = 'ACCEPTED'", Long.class);
        assertThat(stats.acceptedMatches()).isEqualTo(accepted);

        double expected = stats.totalMatches() == 0
                ? 0.0
                : Math.round(accepted * 1000.0 / stats.totalMatches()) / 10.0;
        assertThat(stats.acceptanceRate()).isEqualTo(expected);
    }

    @Test
    void 요청자_성별_집계가_DB_집계와_일치한다() {
        cacheManager.getCache("matchStats").clear();
        MatchStatsResponse stats = adminStatsService.matchStats();

        Long male = jdbcTemplate.queryForObject(
                "select count(*) from match_records m join users u on u.id = m.requester_id where u.gender = 'MALE'",
                Long.class);
        assertThat(stats.byGender().getOrDefault("MALE", 0L)).isEqualTo(male);
    }

    @Test
    void 일별_집계_키는_ISO_날짜_문자열이다() {
        cacheManager.getCache("matchStats").clear();
        MatchStatsResponse stats = adminStatsService.matchStats();

        assertThat(stats.daily()).isNotEmpty();
        assertThat(stats.daily().keySet())
                .allSatisfy(key -> assertThat(LocalDate.parse(key, DateTimeFormatter.ISO_LOCAL_DATE)).isNotNull());
        // 오름차순 정렬 보장 (DB order by)
        assertThat(List.copyOf(stats.daily().keySet())).isSorted();
    }
}
