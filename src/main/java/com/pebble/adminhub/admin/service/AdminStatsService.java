package com.pebble.adminhub.admin.service;

import com.pebble.adminhub.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.adminhub.matching.domain.MatchStatus;
import com.pebble.adminhub.matching.repository.MatchRecordRepository;
import com.pebble.adminhub.matching.repository.MatchRecordRepository.CountRow;
import com.pebble.adminhub.matching.repository.MatchRecordRepository.DailyCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MatchRecordRepository matchRecordRepository;

    /**
     * 일별/성별(요청자 기준)/상태별 매칭 현황 요약.
     * 집계는 DB의 GROUP BY가 수행하며(인덱스: V4), 애플리케이션은 결과 행만 Map으로 옮긴다.
     * 전체/성사 건수와 성사율은 상태별 집계에서 파생되므로 추가 쿼리가 필요 없다 — 총 3쿼리.
     * 결과는 60초 TTL 캐시에 담기고, 매칭 상태 변경(만료 배치) 시점에 즉시 무효화된다.
     */
    @Cacheable("matchStats")
    public MatchStatsResponse matchStats() {
        Map<String, Long> byStatus = toMap(matchRecordRepository.countGroupByStatus());
        Map<String, Long> byGender = toMap(matchRecordRepository.countGroupByRequesterGender());

        Map<String, Long> daily = new LinkedHashMap<>();
        for (DailyCountRow row : matchRecordRepository.countGroupByDay()) {
            daily.put(row.getBucket().format(DATE), row.getCnt());
        }

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long accepted = byStatus.getOrDefault(MatchStatus.ACCEPTED.name(), 0L);
        double acceptanceRate = total == 0 ? 0.0 : Math.round(accepted * 1000.0 / total) / 10.0;

        return new MatchStatsResponse(total, accepted, acceptanceRate, daily, byGender, byStatus);
    }

    private static Map<String, Long> toMap(List<CountRow> rows) {
        Map<String, Long> result = new TreeMap<>();
        for (CountRow row : rows) {
            result.put(row.getBucket(), row.getCnt());
        }
        return result;
    }
}
