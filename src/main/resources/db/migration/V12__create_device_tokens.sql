-- FCM 기기 토큰 (사용자 한 명이 여러 기기, 브라우저를 쓸 수 있음)
CREATE TABLE device_tokens (
    device_token_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    token           VARCHAR(500) NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (device_token_id),
    UNIQUE KEY uk_device_tokens_token (token),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);