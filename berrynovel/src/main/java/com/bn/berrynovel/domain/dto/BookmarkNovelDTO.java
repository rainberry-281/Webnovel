package com.bn.berrynovel.domain.dto;

import java.util.List;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;

public class BookmarkNovelDTO {
    private final Novel novel;
    private final List<Chapter> chapters;

    public BookmarkNovelDTO(Novel novel, List<Chapter> chapters) {
        this.novel = novel;
        this.chapters = chapters;
    }

    public Novel getNovel() {
        return novel;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }
}
