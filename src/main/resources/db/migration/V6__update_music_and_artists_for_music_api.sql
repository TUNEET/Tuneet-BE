-- 음악 API 데이터에 맞춰 곡·아티스트 컬럼 변경
ALTER TABLE artists
    MODIFY COLUMN artist_image VARCHAR(300) NULL,
    MODIFY COLUMN external_id  VARCHAR(100) NOT NULL;

ALTER TABLE music
    MODIFY COLUMN title         VARCHAR(1024) NOT NULL,
    MODIFY COLUMN artist        VARCHAR(1024) NOT NULL,
    MODIFY COLUMN music_picture VARCHAR(300)  NULL,
    ADD COLUMN canonical_id VARCHAR(36) NOT NULL AFTER external_url,
    ADD COLUMN artist_id    BIGINT      NULL AFTER canonical_id,
    ADD INDEX idx_music_canonical_id (canonical_id),
    ADD CONSTRAINT fk_music_artist FOREIGN KEY (artist_id) REFERENCES artists (artist_id);
