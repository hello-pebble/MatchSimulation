package com.pebble.mvp.chat.websocket;

import com.pebble.mvp.chat.dto.ChatDtos.ChatMessageResponse;
import com.pebble.mvp.chat.dto.ChatDtos.ChatSendRequest;
import com.pebble.mvp.chat.service.ChatPollRegistry;
import com.pebble.mvp.chat.service.ChatService;
import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * 채팅 WebSocket 핸들러 — 연결 유지형 양방향 채널.
 * 수신: 저장된 새 메시지를 서버가 즉시 push (폴링 불필요)
 * 전송: {"content":"..."} 프레임 → 기존 ChatService.send 재사용(권한·검증·저장 동일)
 * 검증/권한 오류는 {"error":"..."} 프레임으로 회신하고 연결은 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatSessionRegistry sessionRegistry;
    private final ChatPollRegistry chatPollRegistry;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.add(matchIdOf(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage frame) throws IOException {
        Long matchId = matchIdOf(session);
        String content;
        try {
            content = objectMapper.readValue(frame.getPayload(), ChatSendRequest.class).content();
        } catch (RuntimeException e) {
            sendError(session, "잘못된 형식입니다. {\"content\":\"...\"} JSON을 보내세요.");
            return;
        }
        if (content == null || content.isBlank()) {
            sendError(session, "메시지 내용은 비어 있을 수 없습니다.");
            return;
        }
        if (content.length() > 500) {
            sendError(session, "메시지는 500자를 넘을 수 없습니다.");
            return;
        }
        User me = userRepository.findById((Long) session.getAttributes().get(ChatSessionRegistry.ATTR_USER_ID))
                .orElse(null);
        if (me == null) {
            sendError(session, "계정을 찾을 수 없습니다.");
            return;
        }
        try {
            ChatMessageResponse saved = chatService.send(me, matchId, content);
            // 저장 트랜잭션 커밋 후 — WebSocket 세션 push + Long Polling 대기자 깨우기 (수신 경로 교차 호환)
            sessionRegistry.broadcast(matchId, saved);
            chatPollRegistry.publish(matchId, chatService);
        } catch (ApiException e) {
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.remove(matchIdOf(session), session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 전송 오류: session={}", session.getId(), exception);
        sessionRegistry.remove(matchIdOf(session), session);
    }

    private Long matchIdOf(WebSocketSession session) {
        return (Long) session.getAttributes().get(ChatSessionRegistry.ATTR_MATCH_ID);
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("error", message))));
    }
}
