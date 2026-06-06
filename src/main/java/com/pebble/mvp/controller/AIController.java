package com.pebble.mvp.controller;

import com.pebble.mvp.dto.AIResponse;
import com.pebble.mvp.dto.InterviewRequest;
import com.pebble.mvp.dto.SimulateRequest;
import com.pebble.mvp.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
    private final AIService aiService;

    @PostMapping("/interview")
    public ResponseEntity<String> conductInterview(@RequestBody InterviewRequest request) {
        return ResponseEntity.ok(aiService.conductInterview(request.getUserId(), request.getAnswer()));
    }

    @PostMapping("/simulate")
    public ResponseEntity<AIResponse> simulateConversation(@RequestBody SimulateRequest request) {
        return ResponseEntity.ok(aiService.simulateConversation(request.getUserId(), request.getMessage()));
    }
}
