-- 사용자 Refresh Token
ALTER TABLE users
    ADD COLUMN refresh_token_hash       VARCHAR(64) NULL COMMENT 'Refresh Token 해시값',
    ADD COLUMN refresh_token_expires_at DATETIME(6) NULL COMMENT 'Refresh Token 만료 일시';
