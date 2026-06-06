package com.pebble.mvp.service;

import com.pebble.mvp.dto.MemberSearchResponse;
import com.pebble.mvp.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final UserProfileRepository userProfileRepository;

    public List<MemberSearchResponse> searchMembers(Long userId) {
        return userProfileRepository.findAll().stream()
                .filter(profile -> !profile.getUserId().equals(userId))
                .map(profile -> MemberSearchResponse.builder()
                        .userId(profile.getUserId())
                        .name(profile.getName())
                        .age(profile.getAge())
                        .job(profile.getJob())
                        .location(profile.getLocation())
                        .matchingRate(70.0 + (Math.random() * 25.0)) // Mock matching rate between 70% and 95%
                        .build())
                .collect(Collectors.toList());
    }
}
