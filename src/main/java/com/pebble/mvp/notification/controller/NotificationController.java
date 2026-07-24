package com.pebble.mvp.notification.controller;

import com.pebble.mvp.notification.dto.NotificationDtos.NotificationResponse;
import com.pebble.mvp.user.service.AuthService;
import com.pebble.mvp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AuthService authService;
    private final NotificationService notificationService;

    @GetMapping("/my")
    public List<NotificationResponse> myNotifications(@RequestHeader("X-AUTH-TOKEN") String token) {
        return notificationService.myNotifications(authService.authenticate(token));
    }
}
