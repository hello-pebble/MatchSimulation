package com.pebble.mvp.chat.controller;

import com.pebble.mvp.chat.dto.ChatDtos.ChatMessageResponse;
import com.pebble.mvp.chat.dto.ChatDtos.ChatRoomResponse;
import com.pebble.mvp.chat.dto.ChatDtos.ChatSendRequest;
import com.pebble.mvp.chat.service.ChatPollRegistry;
import com.pebble.mvp.chat.service.ChatService;
import com.pebble.mvp.chat.websocket.ChatSessionRegistry;
import com.pebble.mvp.user.domain.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@Tag(name = "채팅", description = "매칭 성사(ACCEPTED) 상대와의 1:1 대화 — afterId 증분 조회 + Long Polling")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final long MAX_TIMEOUT_SECONDS = 30;

    private final ChatService chatService;
    private final ChatPollRegistry chatPollRegistry;
    private final ChatSessionRegistry chatSessionRegistry;

    @GetMapping("/rooms")
    public List<ChatRoomResponse> myRooms(@AuthenticationPrincipal User me) {
        return chatService.myRooms(me);
    }

    @PostMapping("/{matchId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@AuthenticationPrincipal User me,
                                    @PathVariable Long matchId,
                                    @Valid @RequestBody ChatSendRequest request) {
        ChatMessageResponse sent = chatService.send(me, matchId, request.content());
        // send()의 트랜잭션이 커밋된 뒤 대기자를 깨운다 — 커밋 전 조회로 메시지를 놓치지 않게
        chatPollRegistry.publish(matchId, chatService);
        // WebSocket으로 수신 중인 세션에도 즉시 push (수신 경로 교차 호환)
        chatSessionRegistry.broadcast(matchId, sent);
        return sent;
    }

    @GetMapping("/{matchId}/messages")
    public List<ChatMessageResponse> messages(@AuthenticationPrincipal User me,
                                              @PathVariable Long matchId,
                                              @RequestParam(required = false) Long afterId) {
        return chatService.messages(me, matchId, afterId);
    }

    /**
     * Long Polling — 새 메시지가 있으면 즉시, 없으면 최대 timeoutSeconds(1~30초, 기본 20초)
     * 대기 후 빈 배열을 반환한다. 권한/상태 오류는 대기 없이 즉시 응답한다.
     */
    @GetMapping("/{matchId}/messages/poll")
    public DeferredResult<List<ChatMessageResponse>> poll(@AuthenticationPrincipal User me,
                                                          @PathVariable Long matchId,
                                                          @RequestParam(defaultValue = "0") Long afterId,
                                                          @RequestParam(defaultValue = "20") long timeoutSeconds) {
        long timeout = Math.clamp(timeoutSeconds, 1, MAX_TIMEOUT_SECONDS);
        List<ChatMessageResponse> immediate = chatService.messages(me, matchId, afterId);

        DeferredResult<List<ChatMessageResponse>> result =
                new DeferredResult<>(timeout * 1000, List.<ChatMessageResponse>of());
        if (!immediate.isEmpty()) {
            result.setResult(immediate);
            return result;
        }
        chatPollRegistry.register(matchId, new ChatPollRegistry.Waiter(me, afterId, result));
        // 조회와 등록 사이에 도착한 메시지 보정 — setResult는 이미 완료된 경우 무시된다
        List<ChatMessageResponse> raced = chatService.messages(me, matchId, afterId);
        if (!raced.isEmpty()) {
            result.setResult(raced);
        }
        return result;
    }
}
