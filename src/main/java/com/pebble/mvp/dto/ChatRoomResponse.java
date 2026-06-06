package com.pebble.mvp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatRoomResponse {
    private Long id;
    private Long otherUserId;
    private String otherUserName;
}
