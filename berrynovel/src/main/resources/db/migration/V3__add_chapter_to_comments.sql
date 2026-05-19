ALTER TABLE Comments
    ADD COLUMN chapter_id BIGINT NULL,
    ADD CONSTRAINT fk_comments_chapter
        FOREIGN KEY (chapter_id) REFERENCES Chapters(id)
        ON DELETE SET NULL;
