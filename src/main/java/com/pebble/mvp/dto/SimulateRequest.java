package com.pebble.mvp.dto;

import lombok.Data;

@Data
public class SimulateRequest {
    private Long userId;
    private String message;
}
