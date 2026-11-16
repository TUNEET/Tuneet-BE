-- 위치 갱신 일시 (7일 넘게 갱신하지 않은 사용자는 추천 후보에서 제외)
ALTER TABLE users
    ADD COLUMN location_updated_at DATETIME(6) NULL;

-- 마지막으로 받은 추천 사용자 (홈·매칭 탭·추천 목록이 같은 사용자를 보여 주도록 새로 받을 때까지 유지)
CREATE TABLE user_recommendations (
    user_recommendation_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                BIGINT      NOT NULL,
    recommended_user_id    BIGINT      NOT NULL,
    display_order          INT         NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (user_recommendation_id),
    UNIQUE KEY uk_user_recommendations_user_recommended (user_id, recommended_user_id),
    CONSTRAINT fk_user_recommendations_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_recommendations_recommended FOREIGN KEY (recommended_user_id) REFERENCES users (user_id)
);
