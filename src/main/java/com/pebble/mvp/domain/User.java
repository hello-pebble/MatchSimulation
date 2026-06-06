package com.pebble.mvp.domain;

import com.pebble.mvp.domain.enums.Role;
import com.pebble.mvp.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String password;
    private Role role;
    private UserStatus status;
}
