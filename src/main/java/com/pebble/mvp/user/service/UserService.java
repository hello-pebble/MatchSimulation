package com.pebble.mvp.user.service;

import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.domain.UserStatus;
import com.pebble.mvp.user.dto.AuthDtos.UserResponse;
import com.pebble.mvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse changeStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다: " + userId));
        user.setStatus(status);
        return UserResponse.from(user);
    }
}
