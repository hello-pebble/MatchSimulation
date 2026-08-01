package com.pebble.mvp.chat;

import com.pebble.mvp.chat.repository.ChatMessageRepository;
import com.pebble.mvp.chat.service.ChatPollRegistry;
import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatLongPollTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MatchRecordRepository matchRecordRepository;
    @Autowired
    ChatMessageRepository chatMessageRepository;
    @Autowired
    ChatPollRegistry chatPollRegistry;

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    private MatchRecord createAcceptedMatch() {
        return matchRecordRepository.save(MatchRecord.builder()
                .requesterId(userRepository.findByEmail("male4@match.com").orElseThrow().getId())
                .partnerId(userRepository.findByEmail("female4@match.com").orElseThrow().getId())
                .status(MatchStatus.ACCEPTED).score(80.0).createdAt(LocalDateTime.now())
                .build());
    }

    private void cleanup(MatchRecord match) {
        chatMessageRepository.deleteAll(chatMessageRepository.findByMatchIdOrderByIdAsc(match.getId()));
        matchRecordRepository.deleteById(match.getId());
    }

    @Test
    void 새_메시지가_이미_있으면_대기_없이_즉시_응답한다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            String sender = login("male4@match.com");
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", sender).contentType("application/json")
                            .content("{\"content\":\"먼저 도착한 메시지\"}"))
                    .andExpect(status().isCreated());

            long start = System.currentTimeMillis();
            MvcResult pending = mockMvc.perform(
                            get("/api/chat/" + match.getId() + "/messages/poll?afterId=0&timeoutSeconds=10")
                                    .header("X-AUTH-TOKEN", login("female4@match.com")))
                    .andReturn();
            pending.getAsyncResult(3000);
            long elapsed = System.currentTimeMillis() - start;

            mockMvc.perform(asyncDispatch(pending))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].content").value("먼저 도착한 메시지"));
            assertThat(elapsed).isLessThan(3000); // 타임아웃(10초)을 기다리지 않았다
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 대기_중_상대가_전송하면_즉시_완료된다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            String waiterToken = login("male4@match.com");
            String senderToken = login("female4@match.com");

            // 새 메시지가 없으므로 대기 상태로 들어간다
            MvcResult pending = mockMvc.perform(
                            get("/api/chat/" + match.getId() + "/messages/poll?afterId=0&timeoutSeconds=20")
                                    .header("X-AUTH-TOKEN", waiterToken))
                    .andReturn();
            assertThat(pending.getRequest().isAsyncStarted()).isTrue();
            assertThat(chatPollRegistry.waitingCount(match.getId())).isEqualTo(1);

            // 상대 전송 → 커밋 후 publish → 대기자 즉시 완료
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", senderToken).contentType("application/json")
                            .content("{\"content\":\"대기 깨우기\"}"))
                    .andExpect(status().isCreated());

            pending.getAsyncResult(5000); // 타임아웃(20초)보다 훨씬 먼저 완료되어야 한다
            mockMvc.perform(asyncDispatch(pending))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].content").value("대기 깨우기"))
                    .andExpect(jsonPath("$[0].mine").value(false));

            assertThat(chatPollRegistry.waitingCount(match.getId())).isZero(); // 완료된 대기자 정리 (E7)
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 비참여자의_poll은_대기_없이_403이다() throws Exception {
        MatchRecord match = createAcceptedMatch();
        try {
            mockMvc.perform(get("/api/chat/" + match.getId() + "/messages/poll")
                            .header("X-AUTH-TOKEN", login("male5@match.com")))
                    .andExpect(status().isForbidden());
            assertThat(chatPollRegistry.waitingCount(match.getId())).isZero();
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 없는_매칭_poll은_404이다() throws Exception {
        mockMvc.perform(get("/api/chat/999999/messages/poll")
                        .header("X-AUTH-TOKEN", login("male4@match.com")))
                .andExpect(status().isNotFound());
    }
}
