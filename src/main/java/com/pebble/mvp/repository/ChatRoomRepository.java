package com.pebble.mvp.repository;

import com.pebble.mvp.domain.ChatRoom;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ChatRoomRepository {
    private final Map<Long, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    public ChatRoom save(ChatRoom chatRoom) {
        if (chatRoom.getId() == null) {
            chatRoom.setId(sequence.getAndIncrement());
        }
        chatRooms.put(chatRoom.getId(), chatRoom);
        return chatRoom;
    }

    public java.util.List<ChatRoom> findByUserId(Long userId) {
        return chatRooms.values().stream()
                .filter(cr -> cr.getUser1Id().equals(userId) || cr.getUser2Id().equals(userId))
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<ChatRoom> findAll() {
        return new java.util.ArrayList<>(chatRooms.values());
    }
}
