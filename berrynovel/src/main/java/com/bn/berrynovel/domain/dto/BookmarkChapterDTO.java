package com.bn.berrynovel.domain.dto;

import com.bn.berrynovel.domain.Chapter;

public class BookmarkChapterDTO {
    private final Chapter chapter;
    private final Integer linePosition;
    private final String paragraphKey;

    public BookmarkChapterDTO(Chapter chapter, Integer linePosition, String paragraphKey) {
        this.chapter = chapter;
        this.linePosition = linePosition;
        this.paragraphKey = paragraphKey;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public Integer getLinePosition() {
        return linePosition;
    }

    public String getParagraphKey() {
        return paragraphKey;
    }
}
