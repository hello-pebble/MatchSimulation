package com.pebble.mvp.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Caffeine 캐시(TTL은 application.yml의 spring.cache.caffeine.spec)와
 * 배치 스케줄러 활성화.
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {
}
