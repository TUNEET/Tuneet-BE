-- 음악
CREATE TABLE music (
    music_id      BIGINT       NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200) NOT NULL,
    artist        VARCHAR(100) NOT NULL,
    music_picture VARCHAR(300) NOT NULL,
    external_id   VARCHAR(100) NOT NULL,
    external_url  VARCHAR(500) NULL,
    PRIMARY KEY (music_id),
    UNIQUE KEY uk_music_external_id (external_id)
);

-- 아티스트
CREATE TABLE artists (
    artist_id    BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(200) NOT NULL,
    artist_image VARCHAR(300) NOT NULL,
    external_id  VARCHAR(100) NULL,
    PRIMARY KEY (artist_id),
    UNIQUE KEY uk_artists_external_id (external_id)
);

-- 장르
CREATE TABLE genres (
    genre_id      BIGINT      NOT NULL AUTO_INCREMENT,
    genre_name    VARCHAR(30) NOT NULL,
    display_order INT         NULL,
    PRIMARY KEY (genre_id),
    UNIQUE KEY uk_genres_genre_name (genre_name)
);

-- 사용자
CREATE TABLE users (
    user_id          BIGINT       NOT NULL AUTO_INCREMENT,
    profile_music_id BIGINT       NULL,
    email            VARCHAR(300) NOT NULL,
    password         VARCHAR(300) NOT NULL,
    nickname         VARCHAR(100) NULL,
    profile_picture  VARCHAR(300) NULL,
    latitude         DOUBLE       NULL,
    longitude        DOUBLE       NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_nickname (nickname),
    CONSTRAINT fk_users_profile_music FOREIGN KEY (profile_music_id) REFERENCES music (music_id)
);

-- 사용자 좋아하는 노래 (1곡 이상)
CREATE TABLE user_musics (
    user_music_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    music_id      BIGINT NOT NULL,
    PRIMARY KEY (user_music_id),
    UNIQUE KEY uk_user_musics_user_music (user_id, music_id),
    CONSTRAINT fk_user_musics_user  FOREIGN KEY (user_id)  REFERENCES users (user_id),
    CONSTRAINT fk_user_musics_music FOREIGN KEY (music_id) REFERENCES music (music_id)
);

-- 사용자 선호 아티스트 (3팀 이상)
CREATE TABLE user_artists (
    user_artist_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id        BIGINT NOT NULL,
    artist_id      BIGINT NOT NULL,
    PRIMARY KEY (user_artist_id),
    UNIQUE KEY uk_user_artists_user_artist (user_id, artist_id),
    CONSTRAINT fk_user_artists_user   FOREIGN KEY (user_id)   REFERENCES users (user_id),
    CONSTRAINT fk_user_artists_artist FOREIGN KEY (artist_id) REFERENCES artists (artist_id)
);

-- 사용자 선호 장르 (1개 이상)
CREATE TABLE user_genres (
    user_genre_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    genre_id      BIGINT NOT NULL,
    PRIMARY KEY (user_genre_id),
    UNIQUE KEY uk_user_genres_user_genre (user_id, genre_id),
    CONSTRAINT fk_user_genres_user  FOREIGN KEY (user_id)  REFERENCES users (user_id),
    CONSTRAINT fk_user_genres_genre FOREIGN KEY (genre_id) REFERENCES genres (genre_id)
);

-- 음원 플랫폼 연동 (YOUTUBE_MUSIC)
CREATE TABLE user_platforms (
    user_platform_id BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL,
    platform         VARCHAR(20)   NOT NULL,
    access_token     VARCHAR(1000) NULL,
    refresh_token    VARCHAR(1000) NULL,
    external_user_id VARCHAR(200)  NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_platform_id),
    UNIQUE KEY uk_user_platforms_user_platform (user_id, platform),
    CONSTRAINT fk_user_platforms_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 사용자 차단 / 숨김 (BLOCK / HIDE)
CREATE TABLE user_blocks (
    user_block_id BIGINT      NOT NULL AUTO_INCREMENT,
    blocker_id    BIGINT      NOT NULL,
    blocked_id    BIGINT      NOT NULL,
    type          VARCHAR(20) NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_block_id),
    UNIQUE KEY uk_user_blocks_blocker_blocked (blocker_id, blocked_id),
    CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users (user_id)
);

-- 피드
CREATE TABLE feeds (
    feed_id     BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    music_id    BIGINT       NOT NULL,
    content     VARCHAR(100) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (feed_id),
    KEY idx_feeds_created_at (created_at),
    CONSTRAINT fk_feeds_user  FOREIGN KEY (user_id)  REFERENCES users (user_id),
    CONSTRAINT fk_feeds_music FOREIGN KEY (music_id) REFERENCES music (music_id)
);

-- 피드 코멘트 (노래와 함께 보내는 메시지)
CREATE TABLE feed_messages (
    feed_message_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    feed_id         BIGINT       NOT NULL,
    music_id        BIGINT       NULL,
    message_content VARCHAR(100) NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (feed_message_id),
    CONSTRAINT fk_feed_messages_user  FOREIGN KEY (user_id)  REFERENCES users (user_id),
    CONSTRAINT fk_feed_messages_feed  FOREIGN KEY (feed_id)  REFERENCES feeds (feed_id),
    CONSTRAINT fk_feed_messages_music FOREIGN KEY (music_id) REFERENCES music (music_id)
);

-- 피드 반응
CREATE TABLE feed_reactions (
    feed_reaction_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    feed_id          BIGINT      NOT NULL,
    emoji            VARCHAR(20) NULL,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (feed_reaction_id),
    UNIQUE KEY uk_feed_reactions_user_feed (user_id, feed_id),
    CONSTRAINT fk_feed_reactions_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_feed_reactions_feed FOREIGN KEY (feed_id) REFERENCES feeds (feed_id)
);

-- 편지
CREATE TABLE letters (
    letter_id           BIGINT       NOT NULL AUTO_INCREMENT,
    sender_id           BIGINT       NOT NULL,
    receiver_id         BIGINT       NOT NULL,
    music_id            BIGINT       NOT NULL,
    letter_content      VARCHAR(100) NOT NULL,
    is_random           BOOLEAN      NOT NULL,
    deleted_by_receiver BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             DATETIME(6)  NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (letter_id),
    KEY idx_letters_sender_created_at (sender_id, created_at),
    KEY idx_letters_receiver_created_at (receiver_id, created_at),
    CONSTRAINT fk_letters_sender   FOREIGN KEY (sender_id)   REFERENCES users (user_id),
    CONSTRAINT fk_letters_receiver FOREIGN KEY (receiver_id) REFERENCES users (user_id),
    CONSTRAINT fk_letters_music    FOREIGN KEY (music_id)    REFERENCES music (music_id)
);

-- 알림 (REACTION / FEED_MESSAGE / LETTER, target_id는 type에 따라 대상 ID)
CREATE TABLE notifications (
    notification_id BIGINT      NOT NULL AUTO_INCREMENT,
    receiver_id     BIGINT      NOT NULL,
    sender_id       BIGINT      NOT NULL,
    type            VARCHAR(20) NOT NULL,
    target_id       BIGINT      NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (notification_id),
    KEY idx_notifications_receiver_created_at (receiver_id, created_at),
    CONSTRAINT fk_notifications_receiver FOREIGN KEY (receiver_id) REFERENCES users (user_id),
    CONSTRAINT fk_notifications_sender   FOREIGN KEY (sender_id)   REFERENCES users (user_id)
);

-- 방
CREATE TABLE rooms (
    room_id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    room_title       VARCHAR(50)  NOT NULL,
    room_picture     VARCHAR(300) NULL,
    password         VARCHAR(100) NULL,
    room_description VARCHAR(200) NULL,
    invite_code      VARCHAR(300) NOT NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (room_id),
    UNIQUE KEY uk_rooms_invite_code (invite_code),
    CONSTRAINT fk_rooms_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 방 멤버 (OWNER / MEMBER)
CREATE TABLE room_users (
    room_user_id BIGINT      NOT NULL AUTO_INCREMENT,
    room_id      BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL,
    PRIMARY KEY (room_user_id),
    UNIQUE KEY uk_room_users_room_user (room_id, user_id),
    CONSTRAINT fk_room_users_room FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    CONSTRAINT fk_room_users_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 방 피드
CREATE TABLE room_posts (
    room_post_id      BIGINT       NOT NULL AUTO_INCREMENT,
    room_id           BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    music_id          BIGINT       NOT NULL,
    room_post_content VARCHAR(100) NOT NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (room_post_id),
    KEY idx_room_posts_room_created_at (room_id, created_at),
    CONSTRAINT fk_room_posts_room  FOREIGN KEY (room_id)  REFERENCES rooms (room_id),
    CONSTRAINT fk_room_posts_user  FOREIGN KEY (user_id)  REFERENCES users (user_id),
    CONSTRAINT fk_room_posts_music FOREIGN KEY (music_id) REFERENCES music (music_id)
);

-- 방 플레이리스트
CREATE TABLE room_playlists (
    room_playlist_id BIGINT      NOT NULL AUTO_INCREMENT,
    room_id          BIGINT      NOT NULL,
    music_id         BIGINT      NOT NULL,
    user_id          BIGINT      NOT NULL,
    position         INT         NOT NULL,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (room_playlist_id),
    KEY idx_room_playlists_room_position (room_id, position),
    CONSTRAINT fk_room_playlists_room  FOREIGN KEY (room_id)  REFERENCES rooms (room_id),
    CONSTRAINT fk_room_playlists_music FOREIGN KEY (music_id) REFERENCES music (music_id),
    CONSTRAINT fk_room_playlists_user  FOREIGN KEY (user_id)  REFERENCES users (user_id)
);
