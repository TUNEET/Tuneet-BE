package com.tuneet.tuneetbe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // 유튜브 좋아요 30일 재동기화
public class SchedulingConfig {
}
