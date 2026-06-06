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
public class Match {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private LocalDateTime createdAt;
}
