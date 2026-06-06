package com.pebble.mvp.dto;

import lombok.Data;

@Data
public class MatchRequest {
    private Long senderId;
    private Long receiverId;
}
