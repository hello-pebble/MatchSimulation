package com.pebble.mvp.chat;

import com.pebble.mvp.chat.repository.ChatMessageRepository;
import com.pebble.mvp.chat.websocket.ChatSessionRegistry;
import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.repository.UserRepository;
import com.pebble.mvp.user.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * WebSocket 실연결 테스트 — 서버를 랜덤 포트로 띄우고 StandardWebSocketClient로 접속한다.
 * 핸드셰이크 인증 거부, 양방향 전송(mine 구분), REST 전송과의 교차 호환,
 * 세션 정리(누수 없음), 검증 실패 error 프레임을 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    ChatMessageRepository chatMessageRepository;
    @Autowired
    ChatSessionRegistry chatSessionRegistry;
    @Autowired
    JwtProvider jwtProvider;

    /** 수신 프레임을 큐에 쌓는 클라이언트 핸들러 */
    static class RecordingHandler extends TextWebSocketHandler {
        final BlockingQueue<String> frames = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            frames.add(message.getPayload());
        }

        JsonNode nextFrame(ObjectMapper mapper) throws InterruptedException {
            String payload = frames.poll(5, TimeUnit.SECONDS);
            assertThat(payload).as("5초 내 프레임 수신").isNotNull();
            return mapper.readTree(payload);
        }
    }

    private User userOf(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String tokenOf(String email) {
        User user = userOf(email);
        return jwtProvider.issue(user.getId(), user.getRole().name());
    }

    private MatchRecord createAcceptedMatch() {
        return matchRecordRepository.save(MatchRecord.builder()
                .requesterId(userOf("male4@match.com").getId())
                .partnerId(userOf("female4@match.com").getId())
                .status(MatchStatus.ACCEPTED).score(80.0).createdAt(LocalDateTime.now())
                .build());
    }

    private void cleanup(MatchRecord match) {
        chatMessageRepository.deleteAll(chatMessageRepository.findByMatchIdOrderByIdAsc(match.getId()));
        matchRecordRepository.deleteById(match.getId());
    }

    private WebSocketSession connect(Long matchId, String token, RecordingHandler handler) throws Exception {
        String url = "ws://localhost:" + port + "/ws/chat?matchId=" + matchId + "&token=" + token;
        return new StandardWebSocketClient().execute(handler, url).get(5, TimeUnit.SECONDS);
    }

    private void awaitSessionCount(Long matchId, int expected) throws InterruptedException {
        for (int i = 0; i < 40 && chatSessionRegistry.sessionCount(matchId) != expected; i++) {
            Thread.sleep(50);
        }
        assertThat(chatSessionRegistry.sessionCount(matchId)).isEqualTo(expected);
    }

    @Test
    void 소켓으로_전송하면_양측이_즉시_수신하고_mine이_구분된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            RecordingHandler maleHandler = new RecordingHandler();
            RecordingHandler femaleHandler = new RecordingHandler();
            WebSocketSession maleWs = connect(match.getId(), tokenOf("male4@match.com"), maleHandler);
            WebSocketSession femaleWs = connect(match.getId(), tokenOf("female4@match.com"), femaleHandler);
            try {
                maleWs.sendMessage(new TextMessage("{\"content\":\"소켓으로 보낸 메시지\"}"));

                JsonNode toMale = maleHandler.nextFrame(objectMapper);
                JsonNode toFemale = femaleHandler.nextFrame(objectMapper);
                assertThat(toMale.get("content").asString()).isEqualTo("소켓으로 보낸 메시지");
                assertThat(toMale.get("mine").asBoolean()).isTrue();     // 보낸 사람 기준
                assertThat(toFemale.get("mine").asBoolean()).isFalse();  // 받는 사람 기준
                assertThat(toFemale.get("senderName").asString()).isEqualTo(userOf("male4@match.com").getName());

                // 저장도 되었는지 (push만 하고 유실되지 않음)
                assertThat(chatMessageRepository.findByMatchIdOrderByIdAsc(match.getId())).hasSize(1);
            } finally {
                maleWs.close();
                femaleWs.close();
            }
        } finally {
            cleanup(match);
        }
    }

    @Test
    void REST_전송도_WebSocket_세션에_즉시_push된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            RecordingHandler handler = new RecordingHandler();
            WebSocketSession ws = connect(match.getId(), tokenOf("female4@match.com"), handler);
            try {
                HttpResponse<String> response = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:" + port + "/api/chat/" + match.getId() + "/messages"))
                                .header("Content-Type", "application/json")
                                .header("X-AUTH-TOKEN", tokenOf("male4@match.com"))
                                .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"REST로 보낸 메시지\"}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(201);

                JsonNode frame = handler.nextFrame(objectMapper);
                assertThat(frame.get("content").asString()).isEqualTo("REST로 보낸 메시지");
                assertThat(frame.get("mine").asBoolean()).isFalse();
            } finally {
                ws.close();
            }
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 토큰이_없거나_위조되면_핸드셰이크가_거부된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            Throwable noToken = catchThrowable(() ->
                    connect(match.getId(), "", new RecordingHandler()));
            Throwable badToken = catchThrowable(() ->
                    connect(match.getId(), "fake.token.value", new RecordingHandler()));
            assertThat(noToken).isNotNull().isInstanceOfAny(Exception.class, CompletionException.class);
            assertThat(badToken).isNotNull();
            assertThat(chatSessionRegistry.sessionCount(match.getId())).isZero();
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 비참여자_토큰은_핸드셰이크가_거부된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            Throwable outsider = catchThrowable(() ->
                    connect(match.getId(), tokenOf("male5@match.com"), new RecordingHandler()));
            assertThat(outsider).isNotNull();
            assertThat(chatSessionRegistry.sessionCount(match.getId())).isZero();
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 연결_종료_시_레지스트리에서_세션이_정리된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            WebSocketSession ws = connect(match.getId(), tokenOf("male4@match.com"), new RecordingHandler());
            awaitSessionCount(match.getId(), 1);
            ws.close();
            awaitSessionCount(match.getId(), 0); // 누수 없음
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 검증_실패는_error_프레임으로_회신되고_연결은_유지된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            RecordingHandler handler = new RecordingHandler();
            WebSocketSession ws = connect(match.getId(), tokenOf("male4@match.com"), handler);
            try {
                ws.sendMessage(new TextMessage("{\"content\":\"\"}"));
                JsonNode error = handler.nextFrame(objectMapper);
                assertThat(error.get("error").asString()).contains("비어 있을 수 없습니다");

                // 연결은 살아 있어 정상 전송이 이어진다
                ws.sendMessage(new TextMessage("{\"content\":\"정상 메시지\"}"));
                JsonNode ok = handler.nextFrame(objectMapper);
                assertThat(ok.get("content").asString()).isEqualTo("정상 메시지");
                assertThat(chatMessageRepository.findByMatchIdOrderByIdAsc(match.getId())).hasSize(1);
            } finally {
                ws.close();
            }
        } finally {
            cleanup(match);
        }
    }
}
