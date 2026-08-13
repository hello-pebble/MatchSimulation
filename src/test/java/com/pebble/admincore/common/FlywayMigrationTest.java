package com.pebble.admincore.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        // information_schema.indexes는 H2 전용이라 JDBC 표준 메타데이터로 조회한다
        // (PostgreSQL/H2 어느 쪽에서 돌든 동일하게 동작).
        assertThat(indexNamesOf("match_records"))
                .contains("IDX_MATCH_RECORDS_CREATED_AT",
                        "IDX_MATCH_RECORDS_STATUS",
                        "IDX_MATCH_RECORDS_REQUESTER_ID");
    }

    @Test
    void 문의_알림_조회_컬럼에_인덱스가_생성된다() {
        assertThat(indexNamesOf("qna")).contains("IDX_QNA_STATUS_CREATED_AT");
        assertThat(indexNamesOf("notifications")).contains("IDX_NOTIFICATIONS_CREATED_AT");
    }

    @Test
    void 마이그레이션된_스키마에_시드_데이터가_적재된다() {
        Long users = jdbcTemplate.queryForObject("select count(*) from users", Long.class);
        Long matches = jdbcTemplate.queryForObject("select count(*) from match_records", Long.class);
        assertThat(users).isEqualTo(21);
        assertThat(matches).isEqualTo(30);
    }

    private List<String> indexNamesOf(String table) {
        return jdbcTemplate.execute((java.sql.Connection connection) -> {
            List<String> names = new ArrayList<>();
            for (String candidate : new String[]{table, table.toUpperCase(Locale.ROOT)}) {
                try (ResultSet rs = connection.getMetaData()
                        .getIndexInfo(null, null, candidate, false, false)) {
                    while (rs.next()) {
                        String name = rs.getString("INDEX_NAME");
                        if (name != null) {
                            names.add(name.toUpperCase(Locale.ROOT));
                        }
                    }
                }
                if (!names.isEmpty()) {
                    break;
                }
            }
            return names;
        });
    }
}
