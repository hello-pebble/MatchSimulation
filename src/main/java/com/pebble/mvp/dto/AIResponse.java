package com.pebble.mvp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIResponse {
    private String response;
    private String feedback;
}
