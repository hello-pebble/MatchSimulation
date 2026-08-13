package com.pebble.adminhub.matching.service;

import com.pebble.adminhub.matching.domain.MatchRecord;
import com.pebble.adminhub.matching.domain.MatchStatus;
import com.pebble.adminhub.matching.repository.MatchRecordRepository;
import com.pebble.adminhub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 응답 없는 매칭 요청 자동 만료 배치.
 * 7일이 지난 REQUESTED 매칭을 EXPIRED로 전이하고 요청자에게 알림을 보낸다.
 * 상태 전이와 알림 생성은 하나의 트랜잭션으로 묶인다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchExpiryScheduler {

    static final int EXPIRY_DAYS = 7;

    private final MatchRecordRepository matchRecordRepository;
    private final NotificationService notificationService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000) // 기동 1분 후, 이후 1시간 주기
    @Transactional
    @CacheEvict(cacheNames = "matchStats", allEntries = true)
    public void expireStaleRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(EXPIRY_DAYS);
        List<MatchRecord> stale = matchRecordRepository
                .findByStatusAndCreatedAtBefore(MatchStatus.REQUESTED, cutoff);
        for (MatchRecord record : stale) {
            record.setStatus(MatchStatus.EXPIRED);
            notificationService.notify(record.getRequesterId(),
                    "매칭 요청 만료", "응답이 없어 매칭 요청이 만료되었습니다. 새로운 상대에게 요청해 보세요.");
        }
        if (!stale.isEmpty()) {
            log.info("매칭 요청 자동 만료: {}건 (기준: {}일 경과)", stale.size(), EXPIRY_DAYS);
        }
    }
}
