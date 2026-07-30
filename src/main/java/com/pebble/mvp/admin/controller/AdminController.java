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
import com.pebble.mvp.user.service.AuthService;
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
 * 관리자 모드 API. 모든 엔드포인트는 X-AUTH-TOKEN의 Role=ADMIN 검사를 거친다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final UserService userService;
    private final QnaService qnaService;
    private final NotificationService notificationService;
    private final AdminStatsService adminStatsService;

    // ── 회원 관리 ──────────────────────────────────────────────

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestHeader("X-AUTH-TOKEN") String token,
                                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {
        authService.requireAdmin(token);
        return userService.findAll(pageable);
    }

    @PatchMapping("/users/{userId}/status")
    public UserResponse changeStatus(@RequestHeader("X-AUTH-TOKEN") String token,
                                     @PathVariable Long userId,
                                     @Valid @RequestBody StatusChangeRequest request) {
        authService.requireAdmin(token);
        return userService.changeStatus(userId, request.status());
    }

    // ── Q&A 관리 ──────────────────────────────────────────────

    @GetMapping("/qna")
    public PageResponse<QnaResponse> qnaList(@RequestHeader("X-AUTH-TOKEN") String token,
                                             @RequestParam(required = false) QnaStatus status,
                                             @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                             Pageable pageable) {
        authService.requireAdmin(token);
        return qnaService.findAll(status, pageable);
    }

    @PostMapping("/qna/{qnaId}/answer")
    public QnaResponse answer(@RequestHeader("X-AUTH-TOKEN") String token,
                              @PathVariable Long qnaId,
                              @Valid @RequestBody QnaAnswerRequest request) {
        authService.requireAdmin(token);
        return qnaService.answer(qnaId, request.answer());
    }

    // ── 알림 등록 ──────────────────────────────────────────────

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createNotification(@RequestHeader("X-AUTH-TOKEN") String token,
                                                   @Valid @RequestBody NotificationCreateRequest request) {
        authService.requireAdmin(token);
        return notificationService.create(request);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications(@RequestHeader("X-AUTH-TOKEN") String token) {
        authService.requireAdmin(token);
        return notificationService.findAll();
    }

    // ── 매칭 현황 통계 ─────────────────────────────────────────

    @GetMapping("/stats/matches")
    public MatchStatsResponse matchStats(@RequestHeader("X-AUTH-TOKEN") String token) {
        authService.requireAdmin(token);
        return adminStatsService.matchStats();
    }
}
