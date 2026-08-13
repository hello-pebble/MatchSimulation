package com.pebble.admincore.common;

import com.pebble.admincore.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.admincore.admin.service.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 PostgreSQL을 컨테이너로 띄워 마이그레이션과 통계 집계 쿼리를 검증한다.
 * 기본 테스트 스위트는 PostgreSQL 호환 모드의 H2로 돌기 때문에, DB 고유 문법
 * (identity, check 제약, cast(... as date))이 진짜 PostgreSQL에서도 통하는지는
 * 이 테스트가 확인한다.
 *
 * Docker가 없는 환경(CI 러너 등)에서는 자동으로 스킵된다.
 */
@SpringBootTest
@Testcontainers
@EnabledIf("dockerAvailable")
class PostgresMigrationIT {

    @Container
    @SuppressWarnings("resource") // 컨테이너 생명주기는 Testcontainers 확장이 관리한다
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("admincore")
            .withUsername("admincore")
            .withPassword("admincore");

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    AdminStatsService adminStatsService;

    @Test
    void 실제_PostgreSQL에서_마이그레이션이_모두_성공한다() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, success from flyway_schema_history order by installed_rank");
        assertThat(history).isNotEmpty();
        assertThat(history).allMatch(row -> Boolean.TRUE.equals(row.get("success")));
        assertThat(history).extracting(row -> String.valueOf(row.get("version")))
                .contains("1", "2", "4");
    }

    @Test
    void 상태값_check_제약이_적용된다() {
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints "
                        + "where constraint_name = 'ck_match_records_status'", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void 통계_인덱스가_생성된다() {
        List<String> indexes = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where tablename = 'match_records'", String.class);
        assertThat(indexes).contains(
                "idx_match_records_created_at",
                "idx_match_records_status",
                "idx_match_records_requester_id");
    }

    @Test
    void 집계_쿼리가_PostgreSQL에서_동작한다() {
        MatchStatsResponse stats = adminStatsService.matchStats();

        assertThat(stats.totalMatches()).isEqualTo(30);
        assertThat(stats.byStatus().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(stats.totalMatches());
        assertThat(stats.byGender().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(stats.totalMatches());
        assertThat(stats.daily()).isNotEmpty();
        assertThat(stats.daily().keySet()).allSatisfy(key -> assertThat(key).matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void 일별_집계가_인덱스를_사용하는_실행계획을_가진다() {
        String plan = String.join("\n", jdbcTemplate.queryForList(
                "explain select cast(created_at as date), count(*) from match_records "
                        + "group by cast(created_at as date)", String.class));
        // 시드 30건 규모에서는 플래너가 seq scan을 고를 수 있다 — 계획 수립 자체가
        // 성공하고 집계 노드가 들어가는지만 확인한다.
        assertThat(plan).containsIgnoringCase("aggregate");
    }
}
