package com.pebble.mvp.admin.dto;

import com.pebble.mvp.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record StatusChangeRequest(@NotNull UserStatus status) {
    }

    public record MatchStatsResponse(
            long totalMatches,
            long acceptedMatches,
            double acceptanceRate,
            Map<String, Long> daily,
            Map<String, Long> byGender,
            Map<String, Long> byStatus
    ) {
    }
}
