package com.pebble.mvp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private Long userId;
    private String name;
    private Integer age;
    private String gender;
    private String job;
    private String location;
    private String aiAnalysisResult;
}
