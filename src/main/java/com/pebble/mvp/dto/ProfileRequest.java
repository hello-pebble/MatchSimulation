package com.pebble.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    private Long userId;
    private String name;
    private Integer age;
    private String gender;
    private String job;
    private String location;
}
