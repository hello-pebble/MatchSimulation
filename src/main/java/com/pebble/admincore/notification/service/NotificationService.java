package com.pebble.admincore.notification.service;

import com.pebble.admincore.common.ApiException;
import com.pebble.admincore.notification.domain.Notification;
import com.pebble.admincore.notification.dto.NotificationDtos.NotificationCreateRequest;
import com.pebble.admincore.notification.dto.NotificationDtos.NotificationResponse;
import com.pebble.admincore.notification.repository.NotificationRepository;
import com.pebble.admincore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationResponse create(NotificationCreateRequest request) {
        if (request.targetUserId() != null && !userRepository.existsById(request.targetUserId())) {
            throw ApiException.notFound("대상 회원을 찾을 수 없습니다: " + request.targetUserId());
        }
        Notification notification = notificationRepository.save(Notification.builder()
                .targetUserId(request.targetUserId())
                .title(request.title())
                .message(request.message())
                .createdAt(LocalDateTime.now())
                .build());
        return NotificationResponse.from(notification);
    }

    /**
     * 서비스 내부용 알림 생성 (매칭 성사, 회원 상태 변경 등).
     * 호출자의 트랜잭션에 참여하므로, 호출 측이 롤백되면 알림도 함께 롤백된다.
     */
    public void notify(Long targetUserId, String title, String message) {
        notificationRepository.save(Notification.builder()
                .targetUserId(targetUserId)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public List<NotificationResponse> findAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
