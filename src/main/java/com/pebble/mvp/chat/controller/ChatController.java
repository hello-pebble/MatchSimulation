package com.pebble.mvp.chat.controller;

import com.pebble.mvp.chat.dto.ChatDtos.ChatMessageResponse;
import com.pebble.mvp.chat.dto.ChatDtos.ChatRoomResponse;
import com.pebble.mvp.chat.dto.ChatDtos.ChatSendRequest;
import com.pebble.mvp.chat.service.ChatService;
import com.pebble.mvp.user.domain.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "채팅", description = "매칭 성사(ACCEPTED) 상대와의 1:1 대화 — afterId 증분 조회")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public List<ChatRoomResponse> myRooms(@AuthenticationPrincipal User me) {
        return chatService.myRooms(me);
    }

    @PostMapping("/{matchId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@AuthenticationPrincipal User me,
                                    @PathVariable Long matchId,
                                    @Valid @RequestBody ChatSendRequest request) {
        return chatService.send(me, matchId, request.content());
    }

    @GetMapping("/{matchId}/messages")
    public List<ChatMessageResponse> messages(@AuthenticationPrincipal User me,
                                              @PathVariable Long matchId,
                                              @RequestParam(required = false) Long afterId) {
        return chatService.messages(me, matchId, afterId);
    }
}
