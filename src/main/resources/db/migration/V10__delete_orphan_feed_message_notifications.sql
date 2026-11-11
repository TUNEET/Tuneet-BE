-- 피드 삭제 시 메시지 알림을 함께 삭제하기 전(#33 이전)에 남은, 대상 메시지가 없는 피드 메시지 알림 정리
DELETE n FROM notifications n
LEFT JOIN feed_messages fm ON fm.feed_message_id = n.target_id
WHERE n.type = 'FEED_MESSAGE'
  AND fm.feed_message_id IS NULL;
