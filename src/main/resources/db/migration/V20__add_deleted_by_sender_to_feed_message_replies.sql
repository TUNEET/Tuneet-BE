-- 보낸 답장을 보낸 사람의 보낸 목록에서만 숨김 (받은 사람의 알림, 답장 상세에는 그대로 남음)
ALTER TABLE feed_message_replies
    ADD COLUMN deleted_by_sender BOOLEAN NOT NULL DEFAULT FALSE;
