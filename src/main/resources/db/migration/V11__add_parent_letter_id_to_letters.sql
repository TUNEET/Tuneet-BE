-- 답장이면 답장 대상인 바로 이전 편지 ID, 일반 편지면 NULL
-- 한 편지에는 답장을 한 번만 보낼 수 있도록 UNIQUE (NULL은 중복으로 보지 않음)
ALTER TABLE letters
    ADD COLUMN parent_letter_id BIGINT NULL,
    ADD CONSTRAINT uk_letters_parent_letter UNIQUE (parent_letter_id),
    ADD CONSTRAINT fk_letters_parent FOREIGN KEY (parent_letter_id) REFERENCES letters (letter_id);
