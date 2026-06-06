package com.pebble.mvp.controller;

import com.pebble.mvp.domain.UserProfile;
import com.pebble.mvp.dto.ProfileRequest;
import com.pebble.mvp.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final UserProfileRepository userProfileRepository;

    @PostMapping
    public ResponseEntity<UserProfile> createProfile(@RequestBody ProfileRequest request) {
        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .age(request.getAge())
                .gender(request.getGender())
                .job(request.getJob())
                .location(request.getLocation())
                .build();
        return ResponseEntity.ok(userProfileRepository.save(profile));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
