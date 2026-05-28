package com.bn.berrynovel.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.util.SlugUtil;

@Service("urlSlugService")
public class UrlSlugService {
    private static final Pattern NOVEL_SLUG_PATTERN = Pattern.compile("^(\\d+)-[a-z0-9-]+$");
    private static final Pattern CHAPTER_SLUG_PATTERN = Pattern.compile("^c(\\d+)-[a-z0-9-]+$");

    public String buildNovelSlug(Novel novel) {
        if (novel == null) {
            throw new IllegalArgumentException("Novel is required");
        }
        return this.toNovelSlug(novel.getId(), novel.getTitle());
    }

    public String buildChapterSlug(Chapter chapter) {
        if (chapter == null) {
            throw new IllegalArgumentException("Chapter is required");
        }
        return this.toChapterSlug(chapter.getId(), chapter.getTitle());
    }

    public String buildReaderUrl(Novel novel, Chapter chapter) {
        return "/" + this.buildNovelSlug(novel) + "/" + this.buildChapterSlug(chapter);
    }

    public String buildNovelUrl(Novel novel) {
        return "/" + this.buildNovelSlug(novel);
    }

    public String toNovelSlug(Long id, String title) {
        return this.toSlug(id, "", title);
    }

    public String toChapterSlug(Long id, String title) {
        return this.toSlug(id, "c", title);
    }

    public Long extractNovelIdFromSlug(String novelSlug) {
        return this.extractId(novelSlug, NOVEL_SLUG_PATTERN, 1);
    }

    public Long extractChapterIdFromSlug(String chapterSlug) {
        return this.extractId(chapterSlug, CHAPTER_SLUG_PATTERN, 1);
    }

    public Long parseIdFromSlug(String slug) {
        return this.extractNovelIdFromSlug(slug);
    }

    private Long extractId(String slug, Pattern pattern, int groupIndex) {
        if (slug == null) {
            return null;
        }

        var matcher = pattern.matcher(slug);
        if (!matcher.matches()) {
            return null;
        }

        try {
            return Long.parseLong(matcher.group(groupIndex));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toSlug(Long id, String prefix, String title) {
        if (id == null) {
            throw new IllegalArgumentException("Id is required");
        }

        String cleanTitle = SlugUtil.toSlug(title);
        if (cleanTitle.isEmpty()) {
            cleanTitle = "untitled";
        }
        return prefix + id + "-" + cleanTitle;
    }
}
