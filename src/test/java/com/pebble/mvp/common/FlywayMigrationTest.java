package com.pebble.mvp.common;

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
    void V3_채팅_테이블_마이그레이션이_성공_이력으로_기록된다() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select \"version\", \"success\" from \"flyway_schema_history\" order by \"installed_rank\"");
        assertThat(history)
                .anyMatch(row -> "3".equals(row.get("version")) && Boolean.TRUE.equals(row.get("success")));
        Long chatMessages = jdbcTemplate.queryForObject("select count(*) from chat_messages", Long.class);
        assertThat(chatMessages).isGreaterThanOrEqualTo(0); // 테이블 존재 확인 (시드 대화 포함 가능)
    }

    @Test
    void 마이그레이션된_스키마에_시드_데이터가_적재된다() {
        Long users = jdbcTemplate.queryForObject("select count(*) from users", Long.class);
        Long matches = jdbcTemplate.queryForObject("select count(*) from match_records", Long.class);
        assertThat(users).isEqualTo(21);
        assertThat(matches).isEqualTo(30);
    }
}
