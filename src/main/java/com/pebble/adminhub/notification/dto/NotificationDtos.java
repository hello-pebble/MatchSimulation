package com.pebble.adminhub.notification.dto;

import com.pebble.adminhub.notification.domain.Notification;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    /** targetUserId가 null이면 전체 회원 대상 공지 */
    public record NotificationCreateRequest(
            Long targetUserId,
            @NotBlank String title,
            @NotBlank String message
    ) {
    }

    public record NotificationResponse(
            Long id,
            Long targetUserId,
            String target,
            String title,
            String message,
            LocalDateTime createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getTargetUserId(),
                    n.getTargetUserId() == null ? "ALL" : "USER",
                    n.getTitle(),
                    n.getMessage(),
                    n.getCreatedAt()
            );
        }
    }
}
