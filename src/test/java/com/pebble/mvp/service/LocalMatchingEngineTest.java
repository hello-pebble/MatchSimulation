package com.pebble.mvp.service;

import com.pebble.mvp.domain.User;
import com.pebble.mvp.domain.enums.Gender;
import com.pebble.mvp.service.matching.LocalMatchingEngine;
import com.pebble.mvp.service.matching.ScoredCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMatchingEngineTest {

    private final LocalMatchingEngine engine = new LocalMatchingEngine();

    private User user(Long id, int age, Gender gender, String job, String location) {
        return User.builder().id(id).name("u" + id).age(age).gender(gender).job(job).location(location).build();
    }

    @Test
    void 지역_나이_직군이_모두_일치할수록_높은_점수로_정렬된다() {
        User me = user(1L, 30, Gender.MALE, "개발자", "서울");
        User best = user(2L, 30, Gender.FEMALE, "개발자", "서울");   // 40+40+20 = 100
        User mid = user(3L, 33, Gender.FEMALE, "디자이너", "서울");  // 40+25 = 65
        User worst = user(4L, 45, Gender.FEMALE, "교사", "부산");    // 0

        List<ScoredCandidate> result = engine.recommend(me, List.of(worst, mid, best));

        assertThat(result).extracting(ScoredCandidate::userId).containsExactly(2L, 3L, 4L);
        assertThat(result.getFirst().score()).isEqualTo(100.0);
        assertThat(result.getLast().score()).isEqualTo(0.0);
    }

    @Test
    void 나이_차이가_8세_이상이면_나이_점수는_0이다() {
        User me = user(1L, 25, Gender.MALE, null, null);
        User candidate = user(2L, 33, Gender.FEMALE, null, null);

        List<ScoredCandidate> result = engine.recommend(me, List.of(candidate));

        assertThat(result.getFirst().score()).isEqualTo(0.0);
    }
}
