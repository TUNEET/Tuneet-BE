-- 방장은 room_users.role(OWNER)로만 관리하므로 rooms.user_id 삭제
ALTER TABLE rooms
    DROP FOREIGN KEY fk_rooms_user;

ALTER TABLE rooms
    DROP COLUMN user_id;
