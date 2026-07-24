package com.pebble.mvp.service.matching;

import com.pebble.mvp.domain.User;

public record ScoredCandidate(
        Long userId,
        String name,
        Integer age,
        String gender,
        String job,
        String location,
        double score,
        String reason
) {
    public static ScoredCandidate of(User user, double score, String reason) {
        return new ScoredCandidate(
                user.getId(),
                user.getName(),
                user.getAge(),
                user.getGender() == null ? null : user.getGender().name(),
                user.getJob(),
                user.getLocation(),
                score,
                reason
        );
    }
}
