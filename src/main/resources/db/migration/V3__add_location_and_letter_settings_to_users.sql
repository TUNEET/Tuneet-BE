-- 사용자 위치정보 동의 및 설정
ALTER TABLE users
    ADD COLUMN location_agreed_at DATETIME(6) NULL COMMENT '위치정보 수집 및 이용 동의 일시 (선택)',
    ADD COLUMN location_visible   BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '위치정보 노출 여부',
    ADD COLUMN letter_receivable  BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '편지 받기 여부';
