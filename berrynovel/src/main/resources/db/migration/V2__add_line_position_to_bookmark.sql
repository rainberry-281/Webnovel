ALTER TABLE Bookmark
    ADD COLUMN line_position INT NULL COMMENT 'Rendered paragraph index saved by reader',
    ADD COLUMN paragraph_key VARCHAR(255) NULL COMMENT 'Stable rendered paragraph key',
    ADD COLUMN updated_at DATETIME NULL COMMENT 'Last bookmark update time';
