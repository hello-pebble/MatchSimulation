package com.pebble.mvp.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 인증_없이_OpenAPI_문서에_접근할_수_있고_전_모듈_경로가_노출된다() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(docs)
                .contains("/api/auth/login")
                .contains("/api/matching/recommendations")
                .contains("/api/qna")
                .contains("/api/notifications/my")
                .contains("/api/admin/stats/matches")
                .contains("X-AUTH-TOKEN"); // SecurityScheme 등록 확인
    }

    @Test
    void swagger_ui가_인증_없이_열린다() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()); // → /swagger-ui/index.html
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
