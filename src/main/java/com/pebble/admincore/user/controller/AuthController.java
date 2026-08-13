package com.pebble.admincore.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.pebble.admincore.user.domain.User;
import com.pebble.admincore.user.dto.AuthDtos.LoginRequest;
import com.pebble.admincore.user.dto.AuthDtos.LoginResponse;
import com.pebble.admincore.user.dto.AuthDtos.SignupRequest;
import com.pebble.admincore.user.dto.AuthDtos.UserResponse;
import com.pebble.admincore.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원/인증", description = "회원가입, 로그인(JWT 발급), 내 정보")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User me) {
        return UserResponse.from(me);
    }
}
