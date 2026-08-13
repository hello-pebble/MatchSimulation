package com.pebble.adminhub.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void V1_마이그레이션이_성공_이력으로_기록된다() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select \"version\", \"description\", \"success\" from \"flyway_schema_history\" order by \"installed_rank\"");
        assertThat(history)
                .anyMatch(row -> "1".equals(row.get("version"))
                        && "init".equals(row.get("description"))
                        && Boolean.TRUE.equals(row.get("success")));
    }

    @Test
    void V4_통계_인덱스_마이그레이션이_성공_이력으로_기록된다() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select \"version\", \"success\" from \"flyway_schema_history\" order by \"installed_rank\"");
        assertThat(history)
                .anyMatch(row -> "4".equals(row.get("version")) && Boolean.TRUE.equals(row.get("success")));
    }

    @Test
    void 통계_집계_컬럼에_인덱스가_생성된다() {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "select index_name from information_schema.indexes where table_name = 'MATCH_RECORDS'");
        assertThat(indexes)
                .extracting(row -> String.valueOf(row.get("INDEX_NAME")).toUpperCase())
                .contains("IDX_MATCH_RECORDS_CREATED_AT",
                        "IDX_MATCH_RECORDS_STATUS",
                        "IDX_MATCH_RECORDS_REQUESTER_ID");
    }

    @Test
    void 마이그레이션된_스키마에_시드_데이터가_적재된다() {
        Long users = jdbcTemplate.queryForObject("select count(*) from users", Long.class);
        Long matches = jdbcTemplate.queryForObject("select count(*) from match_records", Long.class);
        assertThat(users).isEqualTo(21);
        assertThat(matches).isEqualTo(30);
    }
}
