package com.pebble.mvp.chat.repository;

import com.pebble.mvp.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByMatchIdOrderByIdAsc(Long matchId);
    List<ChatMessage> findByMatchIdAndIdGreaterThanOrderByIdAsc(Long matchId, Long afterId);
    Optional<ChatMessage> findTopByMatchIdOrderByIdDesc(Long matchId);
}
