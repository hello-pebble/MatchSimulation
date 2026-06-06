package com.pebble.mvp.controller;

import com.pebble.mvp.domain.User;
import com.pebble.mvp.domain.enums.UserStatus;
import com.pebble.mvp.repository.ChatRoomRepository;
import com.pebble.mvp.repository.MatchRepository;
import com.pebble.mvp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final MatchRepository matchRepository; // Assuming we add a way to count matches
    private final ChatRoomRepository chatRoomRepository; // Assuming we add a way to count chat rooms

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.findAll().size());
        // For simplicity, we'll just count what's in the repositories if we had a count method, 
        // but since they are in-memory maps, we can use findAll().size() if we add findAll() to all.
        // Let's add count or findAll to MatchRepository and ChatRoomRepository if needed.
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUserList() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<User> banUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setStatus(UserStatus.BANNED);
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
