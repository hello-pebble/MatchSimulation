package com.pebble.admincore.notification.repository;

import com.pebble.admincore.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
