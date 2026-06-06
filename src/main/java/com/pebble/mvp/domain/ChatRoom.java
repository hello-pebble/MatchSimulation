package com.pebble.mvp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private LocalDateTime createdAt;
}
