package com.pebble.admincore.matching.repository;

import com.pebble.admincore.matching.domain.MatchRecord;
import com.pebble.admincore.matching.domain.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    List<MatchRecord> findByStatusAndCreatedAtBefore(MatchStatus status, LocalDateTime cutoff);

    // ── 관리자 통계 집계 ────────────────────────────────────────
    // 전건을 애플리케이션으로 끌어와 스트림으로 세지 않고 DB에서 GROUP BY로 집계한다.
    // 각 쿼리는 V4에서 추가한 인덱스(created_at / status / requester_id)를 탄다.

    /** 상태별 매칭 건수 — 전체/성사 건수와 성사율도 이 결과에서 파생된다. */
    @Query(value = """
            select m.status as bucket, count(*) as cnt
            from match_records m
            group by m.status
            order by m.status
            """, nativeQuery = true)
    List<CountRow> countGroupByStatus();

    /** 요청자 성별별 매칭 건수 — 성별 미상/탈퇴 회원은 UNKNOWN으로 집계한다. */
    @Query(value = """
            select coalesce(u.gender, 'UNKNOWN') as bucket, count(*) as cnt
            from match_records m
            left join users u on u.id = m.requester_id
            group by coalesce(u.gender, 'UNKNOWN')
            order by 1
            """, nativeQuery = true)
    List<CountRow> countGroupByRequesterGender();

    /** 일자별 매칭 건수 (오름차순). */
    @Query(value = """
            select cast(m.created_at as date) as bucket, count(*) as cnt
            from match_records m
            group by cast(m.created_at as date)
            order by 1
            """, nativeQuery = true)
    List<DailyCountRow> countGroupByDay();

    /** 문자열 키 집계 결과 한 행 */
    interface CountRow {
        String getBucket();

        long getCnt();
    }

    /** 날짜 키 집계 결과 한 행 (day는 H2 예약어라 별칭은 bucket을 쓴다) */
    interface DailyCountRow {
        LocalDate getBucket();

        long getCnt();
    }
}
