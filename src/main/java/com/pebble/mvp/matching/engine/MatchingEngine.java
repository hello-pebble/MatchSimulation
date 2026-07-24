package com.pebble.mvp.matching.engine;

import com.pebble.mvp.user.domain.User;

import java.util.List;

/**
 * 매칭 추천 엔진 추상화.
 * 외부 AI 매칭 모델/서버 연동 접점 — 구현체 교체는 application.yml의
 * {@code matching.engine} 프로퍼티(local | external-ai)로 이루어진다.
 */
public interface MatchingEngine {

    List<ScoredCandidate> recommend(User me, List<User> candidates);
}
