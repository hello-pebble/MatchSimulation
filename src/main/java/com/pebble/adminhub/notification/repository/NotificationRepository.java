package com.pebble.adminhub.notification.repository;

import com.pebble.adminhub.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
