-- 유튜브 뮤직 연동: 권한 범위와 마지막 동기화 일시
-- access token은 저장하지 않고 refresh token으로 필요할 때 발급
ALTER TABLE user_platforms
    DROP COLUMN access_token,
    ADD COLUMN scope          VARCHAR(500) NULL,
    ADD COLUMN last_synced_at DATETIME(6)  NULL;

-- 좋아요한 노래 (앱 하트, 외부 플랫폼에서 불러온 좋아요)
-- 같은 곡도 출처가 다르면 따로 저장해 연동 해제 때 그 출처만 지움
CREATE TABLE user_music_likes (
    user_music_like_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL,
    music_id           BIGINT      NOT NULL,
    source             VARCHAR(20) NOT NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_music_like_id),
    UNIQUE KEY uk_user_music_likes_user_music_source (user_id, music_id, source),
    CONSTRAINT fk_user_music_likes_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_music_likes_music FOREIGN KEY (music_id) REFERENCES music (music_id)
);

-- 유튜브 영상과 곡의 매칭 결과 (모든 사용자 공유, 30일 지나면 다시 매칭)
CREATE TABLE youtube_video_matches (
    video_id   VARCHAR(32) NOT NULL,
    music_id   BIGINT      NOT NULL,
    is_cover   BOOLEAN     NOT NULL,
    matched_at DATETIME(6) NOT NULL,
    PRIMARY KEY (video_id),
    CONSTRAINT fk_youtube_video_matches_music FOREIGN KEY (music_id) REFERENCES music (music_id)
);
