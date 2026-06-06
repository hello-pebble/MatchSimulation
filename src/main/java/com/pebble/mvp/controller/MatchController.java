package com.pebble.mvp.controller;

import com.pebble.mvp.domain.ChatRoom;
import com.pebble.mvp.dto.ChatRoomResponse;
import com.pebble.mvp.dto.MatchRequest;
import com.pebble.mvp.dto.MemberSearchResponse;
import com.pebble.mvp.repository.ChatRoomRepository;
import com.pebble.mvp.repository.UserProfileRepository;
import com.pebble.mvp.service.MatchingService;
import com.pebble.mvp.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchingService matchingService;
    private final MemberService memberService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserProfileRepository userProfileRepository;

    @PostMapping("/request")
    public ResponseEntity<Boolean> requestMatch(@RequestBody MatchRequest request) {
        return ResponseEntity.ok(matchingService.requestMatch(request.getSenderId(), request.getReceiverId()));
    }

    @GetMapping("/chat-rooms/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(@PathVariable Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);
        List<ChatRoomResponse> response = chatRooms.stream()
                .map(cr -> {
                    Long otherUserId = cr.getUser1Id().equals(userId) ? cr.getUser2Id() : cr.getUser1Id();
                    String otherUserName = userProfileRepository.findByUserId(otherUserId)
                            .map(p -> p.getName())
                            .orElse("Unknown");
                    return ChatRoomResponse.builder()
                            .id(cr.getId())
                            .otherUserId(otherUserId)
                            .otherUserName(otherUserName)
                            .build();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members/{userId}")
    public ResponseEntity<List<MemberSearchResponse>> searchMembers(@PathVariable Long userId) {
        return ResponseEntity.ok(memberService.searchMembers(userId));
    }
}
