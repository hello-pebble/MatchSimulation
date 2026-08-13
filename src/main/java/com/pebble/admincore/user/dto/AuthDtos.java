package com.pebble.admincore.user.dto;

import com.pebble.admincore.user.domain.User;
import com.pebble.admincore.user.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String name,
            @NotNull @Min(19) @Max(100) Integer age,
            @NotNull Gender gender,
            String job,
            String location
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(String token, UserResponse user) {
    }

    public record UserResponse(
            Long id,
            String email,
            String name,
            Integer age,
            String gender,
            String job,
            String location,
            String role,
            String status,
            LocalDateTime createdAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getAge(),
                    user.getGender() == null ? null : user.getGender().name(),
                    user.getJob(),
                    user.getLocation(),
                    user.getRole().name(),
                    user.getStatus().name(),
                    user.getCreatedAt()
            );
        }
    }
}
