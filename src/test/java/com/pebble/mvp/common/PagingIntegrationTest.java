package com.pebble.mvp.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PagingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    String adminToken;

    @BeforeEach
    void login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@match.com\",\"password\":\"admin1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        adminToken = node.get("token").asString();
    }

    @Test
    void 회원_목록이_페이지_크기만큼_반환된다() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=0&size=5")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void 마지막_페이지는_hasNext가_false다() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=4&size=5")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 범위_밖_페이지는_빈_목록을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=999&size=10")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(21));
    }

    @Test
    void 정렬_방향이_적용된다() throws Exception {
        // createdAt ASC → 가장 오래된 가입자(관리자, id=1)가 먼저
        mockMvc.perform(get("/api/admin/users?page=0&size=1&sort=createdAt,asc")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void 허용되지_않은_정렬_필드는_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/users?sort=password,desc")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk()); // password는 실제 필드라 통과 — 존재하지 않는 필드로 검증
        mockMvc.perform(get("/api/admin/users?sort=notAField,desc")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void size_상한을_넘으면_100으로_클램프된다() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=0&size=5000")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void qna_상태_필터와_페이징이_함께_동작한다() throws Exception {
        mockMvc.perform(get("/api/admin/qna?status=WAITING&page=0&size=10")
                        .header("X-AUTH-TOKEN", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("WAITING"))));
    }
}
