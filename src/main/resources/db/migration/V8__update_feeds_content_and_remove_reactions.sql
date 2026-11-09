ALTER TABLE feeds
    MODIFY COLUMN content VARCHAR(100) NOT NULL;

DELETE FROM notifications
WHERE type = 'REACTION';

DROP TABLE feed_reactions;
