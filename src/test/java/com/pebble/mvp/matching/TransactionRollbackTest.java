package com.pebble.mvp.matching;

import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.matching.service.MatchingService;
import com.pebble.mvp.notification.service.NotificationService;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

/**
 * 트랜잭션 원자성 증명: 알림 저장이 실패하면 매칭 상태 변경도 롤백되어야 한다.
 */
@SpringBootTest
class TransactionRollbackTest {

    @Autowired
    MatchingService matchingService;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void 알림_저장이_실패하면_매칭_상태_변경도_롤백된다() {
        MatchRecord record = matchRecordRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.REQUESTED)
                .findFirst().orElseThrow();
        User partner = userRepository.findById(record.getPartnerId()).orElseThrow();

        doThrow(new RuntimeException("알림 저장 실패 (시뮬레이션)"))
                .when(notificationService).notify(anyLong(), any(), any());

        assertThatThrownBy(() -> matchingService.respond(partner, record.getId(), true))
                .isInstanceOf(RuntimeException.class);

        // 상태가 REQUESTED로 롤백되어 있어야 한다
        assertThat(matchRecordRepository.findById(record.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.REQUESTED);
    }
}
