package com.bn.berrynovel.domain.dto;

import java.util.List;

import com.bn.berrynovel.domain.Novel;

public class BookmarkNovelDTO {
    private final Novel novel;
    private final List<BookmarkChapterDTO> chapters;

    public BookmarkNovelDTO(Novel novel, List<BookmarkChapterDTO> chapters) {
        this.novel = novel;
        this.chapters = chapters;
    }

    public Novel getNovel() {
        return novel;
    }

    public List<BookmarkChapterDTO> getChapters() {
        return chapters;
    }
}
