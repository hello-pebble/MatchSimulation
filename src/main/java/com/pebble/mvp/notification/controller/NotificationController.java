package com.pebble.mvp.notification.controller;

import com.pebble.mvp.notification.dto.NotificationDtos.NotificationResponse;
import com.pebble.mvp.notification.service.NotificationService;
import com.pebble.mvp.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public List<NotificationResponse> myNotifications(@AuthenticationPrincipal User me) {
        return notificationService.myNotifications(me);
    }
}
