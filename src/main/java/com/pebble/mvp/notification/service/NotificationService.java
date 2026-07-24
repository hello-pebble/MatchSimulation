package com.pebble.mvp.notification.service;

import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.notification.domain.Notification;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.notification.dto.NotificationDtos.NotificationCreateRequest;
import com.pebble.mvp.notification.dto.NotificationDtos.NotificationResponse;
import com.pebble.mvp.notification.repository.NotificationRepository;
import com.pebble.mvp.user.repository.UserRepository;
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

    /** 전체 공지(targetUserId=null) + 본인 대상 알림 */
    public List<NotificationResponse> myNotifications(User me) {
        return notificationRepository
                .findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public List<NotificationResponse> findAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
