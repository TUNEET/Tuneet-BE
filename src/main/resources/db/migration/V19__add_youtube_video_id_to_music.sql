-- 재생용 유튜브 영상 ID (음악 API의 MusicBrainz 링크, 유튜브 좋아요에서 매칭된 원곡 영상)
ALTER TABLE music
    ADD COLUMN youtube_video_id VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD INDEX idx_music_youtube_video_id (youtube_video_id);
