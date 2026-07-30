package com.pebble.mvp.user.security;

import com.pebble.mvp.user.domain.UserStatus;
import com.pebble.mvp.user.repository.UserRepository;
import com.pebble.mvp.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserService userService;

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    @Test
    void 토큰_없이_보호_API를_호출하면_401_JSON을_받는다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 위조된_토큰은_401을_받는다() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("X-AUTH-TOKEN", "fake.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인하면_JWT로_보호_API에_접근할_수_있다() throws Exception {
        String token = login("male1@match.com", "pass1234");
        assertThat(token.split("\\.")).hasSize(3); // JWT 형식(header.payload.signature)
        mockMvc.perform(get("/api/auth/me").header("X-AUTH-TOKEN", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("male1@match.com"));
    }

    @Test
    void USER_권한으로_관리자_API를_호출하면_403이다() throws Exception {
        String token = login("male1@match.com", "pass1234");
        mockMvc.perform(get("/api/admin/users").header("X-AUTH-TOKEN", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void ADMIN_권한은_관리자_API에_접근할_수_있다() throws Exception {
        String token = login("admin@match.com", "admin1234");
        mockMvc.perform(get("/api/admin/users").header("X-AUTH-TOKEN", token))
                .andExpect(status().isOk());
    }

    @Test
    void 비밀번호는_BCrypt_해시로_저장된다() {
        String stored = userRepository.findByEmail("male1@match.com").orElseThrow().getPassword();
        assertThat(stored).startsWith("$2").isNotEqualTo("pass1234");
    }

    @Test
    void 정지된_계정은_유효한_토큰이라도_401이다() throws Exception {
        // 신규 가입(PENDING) → 로그인 → 정지 → 기존 토큰 무효화 확인
        mockMvc.perform(post("/api/auth/signup").contentType("application/json")
                        .content("{\"email\":\"sec-test@match.com\",\"password\":\"pass1234\",\"name\":\"보안테스트\"," +
                                "\"age\":29,\"gender\":\"MALE\",\"job\":\"개발자\",\"location\":\"서울\"}"))
                .andExpect(status().isCreated());
        String token = login("sec-test@match.com", "pass1234");
        Long id = userRepository.findByEmail("sec-test@match.com").orElseThrow().getId();

        userService.changeStatus(id, UserStatus.SUSPENDED);

        mockMvc.perform(get("/api/auth/me").header("X-AUTH-TOKEN", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 정적_콘솔과_h2콘솔은_인증_없이_접근된다() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin.html")).andExpect(status().isOk());
    }
}
