package com.pebble.adminhub.user.service;

import com.pebble.adminhub.common.ApiException;
import com.pebble.adminhub.user.domain.User;
import com.pebble.adminhub.user.domain.Role;
import com.pebble.adminhub.user.domain.UserStatus;
import com.pebble.adminhub.user.dto.AuthDtos.LoginRequest;
import com.pebble.adminhub.user.dto.AuthDtos.LoginResponse;
import com.pebble.adminhub.user.dto.AuthDtos.SignupRequest;
import com.pebble.adminhub.user.dto.AuthDtos.UserResponse;
import com.pebble.adminhub.user.repository.UserRepository;
import com.pebble.adminhub.user.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.badRequest("이미 사용 중인 이메일입니다: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .age(request.age())
                .gender(request.gender())
                .job(request.job())
                .location(request.location())
                .role(Role.USER)
                .status(UserStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 일치하지 않습니다."));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("정지된 계정입니다. 관리자에게 문의하세요.");
        }
        String token = jwtProvider.issue(user.getId(), user.getRole().name());
        return new LoginResponse(token, UserResponse.from(user));
    }
}
