package com.pebble.mvp.user.controller;

import com.pebble.mvp.user.dto.AuthDtos.LoginRequest;
import com.pebble.mvp.user.dto.AuthDtos.LoginResponse;
import com.pebble.mvp.user.dto.AuthDtos.SignupRequest;
import com.pebble.mvp.user.dto.AuthDtos.UserResponse;
import com.pebble.mvp.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public UserResponse me(@RequestHeader("X-AUTH-TOKEN") String token) {
        return UserResponse.from(authService.authenticate(token));
    }
}
