package com.pebble.mvp.chat.websocket;

import com.pebble.mvp.chat.dto.ChatDtos.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * matchId별 WebSocket 세션 레지스트리.
 * Long Polling의 {@code ChatPollRegistry}와 대칭 구조 — 대기자(DeferredResult) 대신
 * 연결이 유지되는 세션을 보관하고, 새 메시지가 저장되면 모든 세션에 즉시 push한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionRegistry {

    /** 핸드셰이크에서 채워지는 세션 attribute 키 */
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USER_NAME = "userName";
    public static final String ATTR_MATCH_ID = "matchId";

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(Long matchId, WebSocketSession session) {
        sessions.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 연결: matchId={}, userId={}, 세션 수={}",
                matchId, session.getAttributes().get(ATTR_USER_ID), sessionCount(matchId));
    }

    public void remove(Long matchId, WebSocketSession session) {
        Set<WebSocketSession> set = sessions.get(matchId);
        if (set != null && set.remove(session)) {
            log.info("WebSocket 종료: matchId={}, userId={}, 세션 수={}",
                    matchId, session.getAttributes().get(ATTR_USER_ID), set.size());
        }
    }

    /**
     * 해당 매칭의 모든 세션에 메시지를 push한다.
     * {@code mine}은 수신자 기준이므로 세션에 보관된 userId로 다시 계산해 직렬화한다.
     */
    public void broadcast(Long matchId, ChatMessageResponse message) {
        Set<WebSocketSession> set = sessions.get(matchId);
        if (set == null) {
            return;
        }
        for (WebSocketSession session : set) {
            if (!session.isOpen()) {
                continue;
            }
            Long viewerId = (Long) session.getAttributes().get(ATTR_USER_ID);
            ChatMessageResponse payload = new ChatMessageResponse(
                    message.id(), message.matchId(), message.senderId(), message.senderName(),
                    message.senderId().equals(viewerId), message.content(), message.createdAt());
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException e) {
                log.warn("WebSocket push 실패: matchId={}, session={}", matchId, session.getId(), e);
            }
        }
    }

    /** 테스트/관측용 — 현재 연결된 세션 수 */
    public int sessionCount(Long matchId) {
        Set<WebSocketSession> set = sessions.get(matchId);
        return set == null ? 0 : set.size();
    }
}
