package com.pebble.mvp.repository;

import com.pebble.mvp.domain.UserProfile;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserProfileRepository {
    private final Map<Long, UserProfile> userProfiles = new ConcurrentHashMap<>();

    public UserProfile save(UserProfile profile) {
        userProfiles.put(profile.getUserId(), profile);
        return profile;
    }

    public Optional<UserProfile> findByUserId(Long userId) {
        return Optional.ofNullable(userProfiles.get(userId));
    }

    public java.util.List<UserProfile> findAll() {
        return new java.util.ArrayList<>(userProfiles.values());
    }
}
