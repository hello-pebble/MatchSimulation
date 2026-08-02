package com.pebble.mvp.chat.websocket;

import com.pebble.mvp.chat.service.ChatService;
import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.domain.UserStatus;
import com.pebble.mvp.user.repository.UserRepository;
import com.pebble.mvp.user.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 핸드셰이크(연결 수립 전) 인증/인가.
 * 브라우저 WebSocket API는 커스텀 헤더를 실을 수 없어 JWT를 쿼리 파라미터(token)로 받는다.
 * 검증 실패 시 연결 자체를 거부한다 — 기존 REST와 동일한 규칙(401/403/404/400)을 재사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        String matchIdParam = params.getFirst("matchId");

        Long userId = jwtProvider.parseUserId(token).orElse(null);
        if (userId == null) {
            return reject(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
        }
        User user = userRepository.findById(userId)
                .filter(u -> u.getStatus() != UserStatus.SUSPENDED)
                .orElse(null);
        if (user == null) {
            return reject(response, HttpStatus.UNAUTHORIZED, "존재하지 않거나 정지된 계정");
        }
        Long matchId;
        try {
            matchId = Long.valueOf(matchIdParam);
        } catch (NumberFormatException e) {
            return reject(response, HttpStatus.BAD_REQUEST, "matchId 파라미터 누락/형식 오류");
        }
        try {
            chatService.verifyParticipant(user, matchId);
        } catch (ApiException e) {
            return reject(response, e.getStatus(), e.getMessage());
        }
        attributes.put(ChatSessionRegistry.ATTR_USER_ID, user.getId());
        attributes.put(ChatSessionRegistry.ATTR_USER_NAME, user.getName());
        attributes.put(ChatSessionRegistry.ATTR_MATCH_ID, matchId);
        return true;
    }

    private boolean reject(ServerHttpResponse response, HttpStatus status, String reason) {
        log.info("WebSocket 핸드셰이크 거부: {} — {}", status.value(), reason);
        response.setStatusCode(status);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
