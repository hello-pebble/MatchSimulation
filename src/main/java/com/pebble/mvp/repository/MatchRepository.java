package com.pebble.mvp.repository;

import com.pebble.mvp.domain.Match;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MatchRepository {
    private final Map<Long, Match> matches = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    public Match save(Match match) {
        if (match.getId() == null) {
            match.setId(sequence.getAndIncrement());
        }
        matches.put(match.getId(), match);
        return match;
    }

    public Optional<Match> findBySenderIdAndReceiverId(Long senderId, Long receiverId) {
        return matches.values().stream()
                .filter(m -> m.getSenderId().equals(senderId) && m.getReceiverId().equals(receiverId))
                .findFirst();
    }

    public java.util.List<Match> findAll() {
        return new java.util.ArrayList<>(matches.values());
    }
}
