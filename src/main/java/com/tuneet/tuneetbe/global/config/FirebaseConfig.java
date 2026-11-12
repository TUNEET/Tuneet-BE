package com.tuneet.tuneetbe.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@Slf4j
public class FirebaseConfig {

    // Firebase 서비스 계정 키 (Base64)
    @Value("${firebase.credentials:}")
    private String credentials;

    // 키가 있을 때만 푸시 전송 객체 생성
    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {

        // 키가 없으면 빈을 만들지 않음
        if (credentials == null || credentials.isBlank()) {
            log.warn("[FirebaseConfig] Firebase 키가 없어 푸시 알림을 비활성화합니다.");
            return null;
        }

        // Base64 키를 JSON으로 복원(.env에는 JSON을 Base64로 바꾼 한 줄 문자열이 들어 있기 때문)
        byte[] decoded = Base64.getDecoder().decode(credentials);
        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));

        // FirebaseApp은 한 번만 초기화
        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {

            // 초기화 -> 나는 누구고, 어느 Firebase 프로젝트에 보낼지를 SDK에게 한 번 알려주는 과정
            app = FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .build());
        }
        else {
            app = FirebaseApp.getInstance();
        }

        return FirebaseMessaging.getInstance(app);
    }
}
