package com.pebble.mvp.service;

import com.pebble.mvp.repository.ChatRoomRepository;
import com.pebble.mvp.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingServiceTest {

    private MatchingService matchingService;
    private MatchRepository matchRepository;
    private ChatRoomRepository chatRoomRepository;

    @BeforeEach
    void setUp() {
        matchRepository = new MatchRepository();
        chatRoomRepository = new ChatRoomRepository();
        matchingService = new MatchingService(matchRepository, chatRoomRepository);
    }

    @Test
    void testRequestMatch_FirstRequest() {
        boolean result = matchingService.requestMatch(1L, 2L);
        assertFalse(result, "First request should not result in a match");
    }

    @Test
    void testRequestMatch_ReciprocalMatch() {
        matchingService.requestMatch(1L, 2L);
        boolean result = matchingService.requestMatch(2L, 1L);
        assertTrue(result, "Reciprocal request should result in a match");
    }
}
