-- 같은 방에 같은 곡 중복 방지 (중복이 이미 있으면 먼저 추가된 것만 남김)
DELETE rp FROM room_playlists rp
JOIN room_playlists keep
  ON keep.room_id = rp.room_id
 AND keep.music_id = rp.music_id
 AND keep.room_playlist_id < rp.room_playlist_id;

ALTER TABLE room_playlists
    ADD CONSTRAINT uk_room_playlists_room_music UNIQUE (room_id, music_id);
