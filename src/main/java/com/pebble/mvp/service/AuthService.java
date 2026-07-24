package com.pebble.mvp.service;

import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.domain.User;
import com.pebble.mvp.domain.enums.Role;
import com.pebble.mvp.domain.enums.UserStatus;
import com.pebble.mvp.dto.AuthDtos.LoginRequest;
import com.pebble.mvp.dto.AuthDtos.LoginResponse;
import com.pebble.mvp.dto.AuthDtos.SignupRequest;
import com.pebble.mvp.dto.AuthDtos.UserResponse;
import com.pebble.mvp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.badRequest("이미 사용 중인 이메일입니다: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .password(request.password())
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
                .filter(u -> u.getPassword().equals(request.password()))
                .orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 일치하지 않습니다."));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("정지된 계정입니다. 관리자에게 문의하세요.");
        }
        String token = tokenStore.issue(user.getId());
        return new LoginResponse(token, UserResponse.from(user));
    }

    /** X-AUTH-TOKEN 헤더의 토큰으로 사용자 식별 */
    public User authenticate(String token) {
        Long userId = tokenStore.resolve(token)
                .orElseThrow(() -> ApiException.unauthorized("유효하지 않은 토큰입니다. 로그인 후 이용하세요."));
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("존재하지 않는 사용자입니다."));
    }

    public User requireAdmin(String token) {
        User user = authenticate(token);
        if (user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("관리자 권한이 필요합니다.");
        }
        return user;
    }
}
