package com.pebble.mvp.matching.engine;

import com.pebble.mvp.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 외부 AI 매칭 서버 어댑터.
 * application.yml에서 {@code matching.engine=external-ai}로 설정하면 활성화된다.
 * 계약: POST {base-url}/api/v1/recommend
 *   요청  {"userId":1, "profile":{...}, "candidates":[{...}]}
 *   응답  [{"userId":2, "name":"...", "age":29, "gender":"FEMALE",
 *          "job":"...", "location":"...", "score":87.5, "reason":"..."}]
 */
@Component
@ConditionalOnProperty(name = "matching.engine", havingValue = "external-ai")
public class ExternalAiMatchingEngine implements MatchingEngine {

    private final RestClient restClient;

    public ExternalAiMatchingEngine(RestClient.Builder builder,
                                    @Value("${matching.ai.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public List<ScoredCandidate> recommend(User me, List<User> candidates) {
        Map<String, Object> request = Map.of(
                "userId", me.getId(),
                "profile", toProfile(me),
                "candidates", candidates.stream().map(this::toProfile).toList()
        );

        List<ScoredCandidate> result = restClient.post()
                .uri("/api/v1/recommend")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return result == null ? List.of() : result;
    }

    private Map<String, Object> toProfile(User user) {
        return Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "age", user.getAge(),
                "gender", user.getGender().name(),
                "job", user.getJob() == null ? "" : user.getJob(),
                "location", user.getLocation() == null ? "" : user.getLocation()
        );
    }
}
