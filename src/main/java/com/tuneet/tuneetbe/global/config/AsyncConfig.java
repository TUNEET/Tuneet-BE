package com.tuneet.tuneetbe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // 푸시 알림을 별도 스레드에서 보내기 위해 @Async 활성화(항상 같이 써야됨)
public class AsyncConfig {
}
