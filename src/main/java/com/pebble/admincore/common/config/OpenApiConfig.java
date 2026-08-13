package com.pebble.admincore.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String AUTH_SCHEME = "X-AUTH-TOKEN";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AdminCore API")
                        .description("운영 관리자 콘솔 백엔드 API — 회원 관리, Q&A 답변, 알림 발송, 매칭 현황 통계. "
                                + "로그인(/api/auth/login) 응답의 JWT를 Authorize 버튼에 입력하면 보호 API를 호출할 수 있다.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(AUTH_SCHEME)
                                .description("로그인으로 발급받은 JWT")))
                .addSecurityItem(new SecurityRequirement().addList(AUTH_SCHEME));
    }
}
