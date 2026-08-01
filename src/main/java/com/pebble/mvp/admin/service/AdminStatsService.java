package com.pebble.mvp.admin.service;

import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MatchRecordRepository matchRecordRepository;
    private final UserRepository userRepository;

    /**
     * 일별/성별(요청자 기준)/상태별 매칭 현황 요약.
     * 전체 매칭·회원을 훑는 집계라 60초 TTL 캐시를 적용하고,
     * 매칭 변경(요청/응답/만료) 시점에 즉시 무효화된다.
     */
    @Cacheable("matchStats")
    public MatchStatsResponse matchStats() {
        var matches = matchRecordRepository.findAll();
        Map<Long, String> genderByUserId = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getGender() == null ? "UNKNOWN" : u.getGender().name()));

        Map<String, Long> daily = matches.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCreatedAt().toLocalDate().format(DATE),
                        TreeMap::new,
                        Collectors.counting()));

        Map<String, Long> byGender = matches.stream()
                .collect(Collectors.groupingBy(
                        m -> genderByUserId.getOrDefault(m.getRequesterId(), "UNKNOWN"),
                        TreeMap::new,
                        Collectors.counting()));

        Map<String, Long> byStatus = matches.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getStatus().name(),
                        TreeMap::new,
                        Collectors.counting()));

        long total = matches.size();
        long accepted = matches.stream()
                .map(MatchRecord::getStatus)
                .filter(s -> s == MatchStatus.ACCEPTED)
                .count();
        double acceptanceRate = total == 0 ? 0.0 : Math.round(accepted * 1000.0 / total) / 10.0;

        return new MatchStatsResponse(total, accepted, acceptanceRate, daily, byGender, byStatus);
    }
}
