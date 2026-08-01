package com.pebble.mvp.chat.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1:1 채팅 메시지. 대화방은 별도 엔티티 없이 ACCEPTED 상태의 MatchRecord.id를 재사용한다.
 * id는 단조 증가(IDENTITY)하므로 클라이언트의 afterId 증분 조회 커서로 쓰인다.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
