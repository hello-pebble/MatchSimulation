package com.pebble.mvp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberSearchResponse {
    private Long userId;
    private String name;
    private Integer age;
    private String job;
    private String location;
    private Double matchingRate;
}
