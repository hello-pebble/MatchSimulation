package com.pebble.mvp.admin.controller;

import com.pebble.mvp.qna.domain.QnaStatus;
import com.pebble.mvp.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.mvp.admin.dto.AdminDtos.StatusChangeRequest;
import com.pebble.mvp.user.dto.AuthDtos.UserResponse;
import com.pebble.mvp.notification.dto.NotificationDtos.NotificationCreateRequest;
import com.pebble.mvp.notification.dto.NotificationDtos.NotificationResponse;
import com.pebble.mvp.qna.dto.QnaDtos.QnaAnswerRequest;
import com.pebble.mvp.qna.dto.QnaDtos.QnaResponse;
import com.pebble.mvp.admin.service.AdminStatsService;
import com.pebble.mvp.notification.service.NotificationService;
import com.pebble.mvp.qna.service.QnaService;
import com.pebble.mvp.user.service.UserService;
import com.pebble.mvp.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 모드 API.
 * 인가는 SecurityConfig의 `/api/admin/** → hasRole('ADMIN')` 규칙이 담당한다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final QnaService qnaService;
    private final NotificationService notificationService;
    private final AdminStatsService adminStatsService;

    // ── 회원 관리 ──────────────────────────────────────────────

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {
        return userService.findAll(pageable);
    }

    @PatchMapping("/users/{userId}/status")
    public UserResponse changeStatus(@PathVariable Long userId,
                                     @Valid @RequestBody StatusChangeRequest request) {
        return userService.changeStatus(userId, request.status());
    }

    // ── Q&A 관리 ──────────────────────────────────────────────

    @GetMapping("/qna")
    public PageResponse<QnaResponse> qnaList(@RequestParam(required = false) QnaStatus status,
                                             @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                             Pageable pageable) {
        return qnaService.findAll(status, pageable);
    }

    @PostMapping("/qna/{qnaId}/answer")
    public QnaResponse answer(@PathVariable Long qnaId,
                              @Valid @RequestBody QnaAnswerRequest request) {
        return qnaService.answer(qnaId, request.answer());
    }

    // ── 알림 등록 ──────────────────────────────────────────────

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createNotification(@Valid @RequestBody NotificationCreateRequest request) {
        return notificationService.create(request);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications() {
        return notificationService.findAll();
    }

    // ── 매칭 현황 통계 ─────────────────────────────────────────

    @GetMapping("/stats/matches")
    public MatchStatsResponse matchStats() {
        return adminStatsService.matchStats();
    }
}
