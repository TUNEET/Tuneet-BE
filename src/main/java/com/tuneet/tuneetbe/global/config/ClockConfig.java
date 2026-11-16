package com.tuneet.tuneetbe.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // 테스트에서 시간을 고정할 수 있도록 현재 시각은 Clock으로 얻음
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
