-- 피드 코멘트 답장 (코멘트 하나에서 두 사용자가 번갈아 주고받는 답장)
-- 바로 이전 메시지는 같은 코멘트의 직전 답장(없으면 코멘트)이라 별도 부모 컬럼을 두지 않음
CREATE TABLE feed_message_replies (
    feed_message_reply_id BIGINT       NOT NULL AUTO_INCREMENT,
    feed_message_id       BIGINT       NOT NULL,
    sender_id             BIGINT       NOT NULL,
    receiver_id           BIGINT       NOT NULL,
    music_id              BIGINT       NOT NULL,
    reply_content         VARCHAR(100) NOT NULL,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (feed_message_reply_id),
    KEY idx_feed_message_replies_message (feed_message_id, feed_message_reply_id),
    CONSTRAINT fk_feed_message_replies_message  FOREIGN KEY (feed_message_id) REFERENCES feed_messages (feed_message_id),
    CONSTRAINT fk_feed_message_replies_sender   FOREIGN KEY (sender_id)       REFERENCES users (user_id),
    CONSTRAINT fk_feed_message_replies_receiver FOREIGN KEY (receiver_id)     REFERENCES users (user_id),
    CONSTRAINT fk_feed_message_replies_music    FOREIGN KEY (music_id)        REFERENCES music (music_id)
);
