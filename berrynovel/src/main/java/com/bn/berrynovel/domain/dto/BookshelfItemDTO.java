package com.bn.berrynovel.domain.dto;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;

public class BookshelfItemDTO {
    private final Novel novel;
    private final Chapter newestChapter;

    public BookshelfItemDTO(Novel novel, Chapter newestChapter) {
        this.novel = novel;
        this.newestChapter = newestChapter;
    }

    public Novel getNovel() {
        return novel;
    }

    public Chapter getNewestChapter() {
        return newestChapter;
    }
}
