package com.pebble.admincore.user;

import com.pebble.admincore.notification.service.NotificationService;
import com.pebble.admincore.user.domain.User;
import com.pebble.admincore.user.domain.UserStatus;
import com.pebble.admincore.user.repository.UserRepository;
import com.pebble.admincore.user.service.UserService;
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
 * 트랜잭션 원자성 증명: 알림 저장이 실패하면 회원 상태 변경도 롤백되어야 한다.
 */
@SpringBootTest
class UserStatusRollbackTest {

    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void 알림_저장이_실패하면_회원_상태_변경도_롤백된다() {
        User target = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.PENDING)
                .findFirst().orElseThrow();

        doThrow(new RuntimeException("알림 저장 실패 (시뮬레이션)"))
                .when(notificationService).notify(anyLong(), any(), any());

        assertThatThrownBy(() -> userService.changeStatus(target.getId(), UserStatus.ACTIVE))
                .isInstanceOf(RuntimeException.class);

        assertThat(userRepository.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.PENDING);
    }
}
