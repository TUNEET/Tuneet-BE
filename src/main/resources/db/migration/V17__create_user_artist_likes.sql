-- 좋아요한 아티스트 (선호 아티스트와 별개, 취향 매칭에도 사용)
CREATE TABLE user_artist_likes (
    user_artist_like_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    artist_id           BIGINT      NOT NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_artist_like_id),
    UNIQUE KEY uk_user_artist_likes_user_artist (user_id, artist_id),
    CONSTRAINT fk_user_artist_likes_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_artist_likes_artist FOREIGN KEY (artist_id) REFERENCES artists (artist_id)
);
