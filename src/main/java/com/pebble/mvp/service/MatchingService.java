package com.pebble.mvp.service;

import com.pebble.mvp.domain.ChatRoom;
import com.pebble.mvp.domain.Match;
import com.pebble.mvp.repository.ChatRoomRepository;
import com.pebble.mvp.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final MatchRepository matchRepository;
    private final ChatRoomRepository chatRoomRepository;

    public boolean requestMatch(Long senderId, Long receiverId) {
        // Save the match request
        Match match = Match.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .createdAt(LocalDateTime.now())
                .build();
        matchRepository.save(match);

        // Check if reciprocal match exists
        boolean reciprocalExists = matchRepository.findBySenderIdAndReceiverId(receiverId, senderId).isPresent();

        if (reciprocalExists) {
            // Create and save ChatRoom
            ChatRoom chatRoom = ChatRoom.builder()
                    .user1Id(senderId)
                    .user2Id(receiverId)
                    .createdAt(LocalDateTime.now())
                    .build();
            chatRoomRepository.save(chatRoom);
            return true;
        }

        return false;
    }
}
