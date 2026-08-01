package com.pebble.mvp.chat;

import com.pebble.mvp.chat.repository.ChatMessageRepository;
import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIntegrationTest {

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

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private MatchRecord createMatch(String requesterEmail, String partnerEmail, MatchStatus status) {
        return matchRecordRepository.save(MatchRecord.builder()
                .requesterId(userId(requesterEmail)).partnerId(userId(partnerEmail))
                .status(status).score(80.0).createdAt(LocalDateTime.now())
                .build());
    }

    /** 공유 컨텍스트 보호 — 테스트가 만든 매칭과 대화를 정리한다 */
    private void cleanup(MatchRecord match) {
        chatMessageRepository.deleteAll(chatMessageRepository.findByMatchIdOrderByIdAsc(match.getId()));
        matchRecordRepository.deleteById(match.getId());
    }

    @Test
    void 전송_후_afterId_증분_조회로_새_메시지만_받는다() throws Exception {
        MatchRecord match = createMatch("male2@match.com", "female2@match.com", MatchStatus.ACCEPTED);
        try {
            String maleToken = login("male2@match.com");
            String femaleToken = login("female2@match.com");

            // male2 전송 → 201, mine=true
            String first = mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", maleToken).contentType("application/json")
                            .content("{\"content\":\"안녕하세요!\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mine").value(true))
                    .andReturn().getResponse().getContentAsString();
            long firstId = objectMapper.readTree(first).get("id").asLong();

            // female2 전체 조회 — 상대 메시지는 mine=false
            mockMvc.perform(get("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", femaleToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].mine").value(false));

            // female2 답장 후, male2는 afterId 증분 조회로 답장만 받는다 (E1)
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", femaleToken).contentType("application/json")
                            .content("{\"content\":\"반가워요~\"}"))
                    .andExpect(status().isCreated());

            String incremental = mockMvc.perform(get("/api/chat/" + match.getId() + "/messages?afterId=" + firstId)
                            .header("X-AUTH-TOKEN", maleToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode arr = objectMapper.readTree(incremental);
            assertThat(arr.size()).isEqualTo(1);
            assertThat(arr.get(0).get("content").asString()).isEqualTo("반가워요~");
            long lastId = arr.get(0).get("id").asLong();

            // 마지막 id 이후는 빈 배열 (E6)
            mockMvc.perform(get("/api/chat/" + match.getId() + "/messages?afterId=" + lastId)
                            .header("X-AUTH-TOKEN", maleToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 참여자가_아니면_403이다() throws Exception {
        MatchRecord match = createMatch("male2@match.com", "female2@match.com", MatchStatus.ACCEPTED);
        try {
            String outsider = login("male3@match.com");
            mockMvc.perform(get("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", outsider))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", outsider).contentType("application/json")
                            .content("{\"content\":\"끼어들기\"}"))
                    .andExpect(status().isForbidden());
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 성사되지_않은_매칭에서는_400이다() throws Exception {
        MatchRecord match = createMatch("male2@match.com", "female3@match.com", MatchStatus.REQUESTED);
        try {
            String token = login("male2@match.com");
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", token).contentType("application/json")
                            .content("{\"content\":\"아직 수락 전\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ACCEPTED")));
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 없는_매칭이면_404이다() throws Exception {
        String token = login("male2@match.com");
        mockMvc.perform(get("/api/chat/999999/messages").header("X-AUTH-TOKEN", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void 빈_내용이나_500자_초과는_400이다() throws Exception {
        MatchRecord match = createMatch("male2@match.com", "female2@match.com", MatchStatus.ACCEPTED);
        try {
            String token = login("male2@match.com");
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", token).contentType("application/json")
                            .content("{\"content\":\"  \"}"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", token).contentType("application/json")
                            .content("{\"content\":\"" + "가".repeat(501) + "\"}"))
                    .andExpect(status().isBadRequest());
        } finally {
            cleanup(match);
        }
    }

    @Test
    void 대화방_목록에_상대_이름과_마지막_메시지가_보인다() throws Exception {
        MatchRecord match = createMatch("male2@match.com", "female2@match.com", MatchStatus.ACCEPTED);
        try {
            String token = login("male2@match.com");
            mockMvc.perform(post("/api/chat/" + match.getId() + "/messages")
                            .header("X-AUTH-TOKEN", token).contentType("application/json")
                            .content("{\"content\":\"마지막 메시지\"}"))
                    .andExpect(status().isCreated());

            String body = mockMvc.perform(get("/api/chat/rooms").header("X-AUTH-TOKEN", token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode rooms = objectMapper.readTree(body);
            JsonNode room = null;
            for (JsonNode r : rooms) {
                if (r.get("matchId").asLong() == match.getId()) room = r;
            }
            assertThat(room).isNotNull();
            assertThat(room.get("partnerId").asLong()).isEqualTo(userId("female2@match.com"));
            assertThat(room.get("lastMessage").asString()).isEqualTo("마지막 메시지");
        } finally {
            cleanup(match);
        }
    }
}
