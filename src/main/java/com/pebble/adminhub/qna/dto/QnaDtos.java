package com.pebble.adminhub.qna.dto;

import com.pebble.adminhub.qna.domain.Qna;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class QnaDtos {

    private QnaDtos() {
    }

    public record QnaAnswerRequest(@NotBlank String answer) {
    }

    public record QnaResponse(
            Long id,
            Long userId,
            String userName,
            String title,
            String question,
            String answer,
            String status,
            LocalDateTime createdAt,
            LocalDateTime answeredAt
    ) {
        public static QnaResponse of(Qna qna, String userName) {
            return new QnaResponse(
                    qna.getId(),
                    qna.getUserId(),
                    userName,
                    qna.getTitle(),
                    qna.getQuestion(),
                    qna.getAnswer(),
                    qna.getStatus().name(),
                    qna.getCreatedAt(),
                    qna.getAnsweredAt()
            );
        }
    }
}
