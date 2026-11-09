-- 사용자 약관 동의 일시
ALTER TABLE users
    ADD COLUMN terms_agreed_at   DATETIME(6) NOT NULL COMMENT '서비스 이용약관 동의 일시',
    ADD COLUMN privacy_agreed_at DATETIME(6) NOT NULL COMMENT '개인정보 처리방침 동의 일시';