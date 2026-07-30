package com.pebble.mvp.matching;

import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.matching.service.MatchingService;
import com.pebble.mvp.notification.repository.NotificationRepository;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.domain.UserStatus;
import com.pebble.mvp.user.repository.UserRepository;
import com.pebble.mvp.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransactionIntegrationTest {

    @Autowired
    MatchingService matchingService;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserService userService;

    private MatchRecord anyRequested() {
        return matchRecordRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.REQUESTED)
                .findFirst()
                .orElseThrow();
    }

    private User partnerOf(MatchRecord record) {
        return userRepository.findById(record.getPartnerId()).orElseThrow();
    }

    @Test
    void 수락하면_상태전이와_양측_알림이_한_트랜잭션으로_생성된다() {
        MatchRecord record = anyRequested();
        long before = notificationRepository.count();

        matchingService.respond(partnerOf(record), record.getId(), true);

        MatchRecord after = matchRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(notificationRepository.count()).isEqualTo(before + 2); // 요청자 + 수락자
        assertThat(notificationRepository.findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(
                record.getRequesterId()))
                .anyMatch(n -> n.getTitle().contains("매칭이 성사"));
    }

    @Test
    void 거절하면_알림_없이_상태만_변경된다() {
        MatchRecord record = anyRequested();
        long before = notificationRepository.count();

        matchingService.respond(partnerOf(record), record.getId(), false);

        assertThat(matchRecordRepository.findById(record.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.REJECTED);
        assertThat(notificationRepository.count()).isEqualTo(before);
    }

    @Test
    void 동시에_두_요청이_응답하면_정확히_한_건만_성공한다() throws Exception {
        MatchRecord record = anyRequested();
        User partner = partnerOf(record);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            boolean accept = i == 0;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    matchingService.respond(partner, record.getId(), accept);
                    success.incrementAndGet();
                } catch (Exception e) { // 낙관적 락(409) 또는 이미 처리(400)
                    failure.incrementAndGet();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(1);
        assertThat(failure.get()).isEqualTo(1);
        assertThat(matchRecordRepository.findById(record.getId()).orElseThrow().getStatus())
                .isIn(MatchStatus.ACCEPTED, MatchStatus.REJECTED);
    }

    @Test
    void 관리자_정지_시_대상_회원_알림이_함께_생성된다() {
        User target = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE && u.getRole().name().equals("USER"))
                .findFirst().orElseThrow();
        long before = notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(target.getId()).size();

        userService.changeStatus(target.getId(), UserStatus.SUSPENDED);

        List<com.pebble.mvp.notification.domain.Notification> after = notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(target.getId());
        assertThat(after.size()).isEqualTo(before + 1);
        assertThat(after.get(0).getTitle()).contains("정지");
    }
}
