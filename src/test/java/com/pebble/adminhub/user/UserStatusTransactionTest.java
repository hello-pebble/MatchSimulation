package com.pebble.adminhub.user;

import com.pebble.adminhub.notification.domain.Notification;
import com.pebble.adminhub.notification.repository.NotificationRepository;
import com.pebble.adminhub.user.domain.User;
import com.pebble.adminhub.user.domain.UserStatus;
import com.pebble.adminhub.user.repository.UserRepository;
import com.pebble.adminhub.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 회원 상태 변경의 트랜잭션 경계 검증 —
 * 상태 전이와 대상 회원 알림 생성이 하나의 트랜잭션으로 묶인다.
 */
@SpringBootTest
class UserStatusTransactionTest {

    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    NotificationRepository notificationRepository;

    private User lastActiveMember() {
        // 다른 테스트가 로그인에 쓰는 계정(male1, admin)을 피해 마지막 ACTIVE 회원을 대상으로 한다
        List<User> actives = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE && u.getRole().name().equals("USER"))
                .toList();
        return actives.get(actives.size() - 1);
    }

    @Test
    void 정지_시_대상_회원_알림이_함께_생성된다() {
        User target = lastActiveMember();
        long before = notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(target.getId()).size();

        userService.changeStatus(target.getId(), UserStatus.SUSPENDED);

        List<Notification> after = notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(target.getId());
        assertThat(after.size()).isEqualTo(before + 1);
        assertThat(after.get(0).getTitle()).contains("정지");

        userService.changeStatus(target.getId(), UserStatus.ACTIVE); // 공유 컨텍스트 상태 원복
    }

    @Test
    void 승인_시_승인_완료_알림이_생성된다() {
        User pending = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.PENDING)
                .findFirst().orElseThrow();

        userService.changeStatus(pending.getId(), UserStatus.ACTIVE);

        assertThat(notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(pending.getId()))
                .anyMatch(n -> n.getTitle().contains("승인"));

        userService.changeStatus(pending.getId(), UserStatus.PENDING); // 공유 컨텍스트 상태 원복
    }

    @Test
    void 같은_상태로_변경하면_알림이_중복_생성되지_않는다() {
        User target = lastActiveMember();
        long before = notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(target.getId()).size();

        userService.changeStatus(target.getId(), UserStatus.ACTIVE); // 이미 ACTIVE

        assertThat(notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(target.getId()))
                .hasSize((int) before);
    }
}
