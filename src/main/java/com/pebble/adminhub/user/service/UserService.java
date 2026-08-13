package com.pebble.adminhub.user.service;

import com.pebble.adminhub.common.ApiException;
import com.pebble.adminhub.user.domain.User;
import com.pebble.adminhub.user.domain.UserStatus;
import com.pebble.adminhub.user.dto.AuthDtos.UserResponse;
import com.pebble.adminhub.user.repository.UserRepository;
import com.pebble.adminhub.common.PageRequests;
import com.pebble.adminhub.notification.service.NotificationService;
import com.pebble.adminhub.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(PageRequests.clamp(pageable)), UserResponse::from);
    }

    /**
     * 상태 변경과 대상 회원 알림 생성을 하나의 트랜잭션으로 처리한다 —
     * 알림 저장이 실패하면 상태 변경도 롤백된다.
     */
    @Transactional
    public UserResponse changeStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다: " + userId));
        UserStatus before = user.getStatus();
        user.setStatus(status);
        if (before != status) {
            if (status == UserStatus.ACTIVE) {
                notificationService.notify(userId, "회원 승인 완료", "회원 승인이 완료되었습니다. 매칭 서비스를 이용할 수 있습니다.");
            } else if (status == UserStatus.SUSPENDED) {
                notificationService.notify(userId, "계정 정지 안내", "계정이 정지되었습니다. 문의는 관리자에게 해주세요.");
            }
        }
        return UserResponse.from(user);
    }
}
