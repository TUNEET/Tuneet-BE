-- 보낸 사람이 보낸 목록에서 삭제했는지 여부 (받은 사람의 편지함, 알림에는 그대로 남음)
ALTER TABLE letters
    ADD COLUMN deleted_by_sender BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE feed_messages
    ADD COLUMN deleted_by_sender BOOLEAN NOT NULL DEFAULT FALSE;
