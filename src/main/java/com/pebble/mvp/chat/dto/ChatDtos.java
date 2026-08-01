package com.pebble.mvp.chat.dto;

import com.pebble.mvp.chat.domain.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ChatDtos {

    public record ChatSendRequest(
            @NotBlank(message = "메시지 내용은 비어 있을 수 없습니다.")
            @Size(max = 500, message = "메시지는 500자를 넘을 수 없습니다.")
            String content
    ) {}

    public record ChatMessageResponse(
            Long id,
            Long matchId,
            Long senderId,
            String senderName,
            boolean mine,
            String content,
            LocalDateTime createdAt
    ) {
        public static ChatMessageResponse of(ChatMessage message, String senderName, Long myId) {
            return new ChatMessageResponse(
                    message.getId(), message.getMatchId(), message.getSenderId(),
                    senderName, message.getSenderId().equals(myId),
                    message.getContent(), message.getCreatedAt());
        }
    }

    /** 대화방 = 내 ACCEPTED 매칭. lastMessage는 없으면 null. */
    public record ChatRoomResponse(
            Long matchId,
            Long partnerId,
            String partnerName,
            String lastMessage,
            LocalDateTime lastMessageAt
    ) {}
}
