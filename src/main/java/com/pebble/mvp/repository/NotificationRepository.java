package com.pebble.mvp.repository;

import com.pebble.mvp.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetUserIdIsNullOrTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
