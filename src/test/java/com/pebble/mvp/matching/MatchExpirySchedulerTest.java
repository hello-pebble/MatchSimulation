package com.pebble.mvp.matching;

import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.matching.service.MatchExpiryScheduler;
import com.pebble.mvp.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MatchExpirySchedulerTest {

    @Autowired
    MatchExpiryScheduler scheduler;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    NotificationRepository notificationRepository;

    @Test
    void 칠일_경과한_REQUESTED_매칭만_만료되고_요청자에게_알림이_간다() {
        MatchRecord stale = matchRecordRepository.save(MatchRecord.builder()
                .requesterId(2L).partnerId(3L)
                .status(MatchStatus.REQUESTED)
                .score(50.0)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build());
        MatchRecord fresh = matchRecordRepository.save(MatchRecord.builder()
                .requesterId(4L).partnerId(5L)
                .status(MatchStatus.REQUESTED)
                .score(50.0)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());
        MatchRecord accepted = matchRecordRepository.save(MatchRecord.builder()
                .requesterId(6L).partnerId(7L)
                .status(MatchStatus.ACCEPTED)
                .score(50.0)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build());

        scheduler.expireStaleRequests();

        assertThat(matchRecordRepository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.EXPIRED);
        assertThat(matchRecordRepository.findById(fresh.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.REQUESTED);
        assertThat(matchRecordRepository.findById(accepted.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.ACCEPTED);
        assertThat(notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(2L))
                .anyMatch(n -> n.getTitle().contains("만료"));

        // 공유 컨텍스트의 매칭 건수 검증 테스트 보호 — 생성 데이터 정리
        matchRecordRepository.deleteById(stale.getId());
        matchRecordRepository.deleteById(fresh.getId());
        matchRecordRepository.deleteById(accepted.getId());
    }
}
