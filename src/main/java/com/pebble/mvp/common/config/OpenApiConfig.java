package com.pebble.mvp.common.config;

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
                        .title("MatchSimulation API")
                        .description("매칭(소개팅) 서비스 백엔드 + 관리자 모드 API. "
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
