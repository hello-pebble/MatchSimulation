package com.pebble.mvp.matching.engine;

import com.pebble.mvp.user.domain.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 규칙 기반 기본 매칭 엔진 (로컬 구현).
 * 지역 일치 +40, 나이 차이(가까울수록 최대 +40), 직군 일치 +20.
 */
@Component
@ConditionalOnProperty(name = "matching.engine", havingValue = "local", matchIfMissing = true)
public class LocalMatchingEngine implements MatchingEngine {

    @Override
    public List<ScoredCandidate> recommend(User me, List<User> candidates) {
        return candidates.stream()
                .map(candidate -> score(me, candidate))
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                .toList();
    }

    private ScoredCandidate score(User me, User candidate) {
        double score = 0;
        List<String> reasons = new ArrayList<>();

        if (me.getLocation() != null && me.getLocation().equals(candidate.getLocation())) {
            score += 40;
            reasons.add("같은 지역(" + candidate.getLocation() + ")");
        }

        if (me.getAge() != null && candidate.getAge() != null) {
            int gap = Math.abs(me.getAge() - candidate.getAge());
            double ageScore = Math.max(0, 40 - gap * 5);
            score += ageScore;
            if (ageScore > 0) {
                reasons.add("나이 차이 " + gap + "세");
            }
        }

        if (me.getJob() != null && Objects.equals(me.getJob(), candidate.getJob())) {
            score += 20;
            reasons.add("같은 직군(" + candidate.getJob() + ")");
        }

        String reason = reasons.isEmpty() ? "기본 후보" : String.join(", ", reasons);
        return ScoredCandidate.of(candidate, score, reason);
    }
}
