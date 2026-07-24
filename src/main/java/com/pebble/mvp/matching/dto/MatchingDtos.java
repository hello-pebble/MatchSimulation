package com.pebble.mvp.matching.dto;

import com.pebble.mvp.matching.domain.MatchRecord;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class MatchingDtos {

    private MatchingDtos() {
    }

    public record MatchRequestDto(@NotNull Long partnerId) {
    }

    public record MatchRespondDto(@NotNull Boolean accept) {
    }

    public record MatchResponse(
            Long id,
            Long requesterId,
            String requesterName,
            Long partnerId,
            String partnerName,
            String status,
            Double score,
            LocalDateTime createdAt
    ) {
        public static MatchResponse of(MatchRecord record, String requesterName, String partnerName) {
            return new MatchResponse(
                    record.getId(),
                    record.getRequesterId(),
                    requesterName,
                    record.getPartnerId(),
                    partnerName,
                    record.getStatus().name(),
                    record.getScore(),
                    record.getCreatedAt()
            );
        }
    }
}
