package com.bn.berrynovel.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service("urlSlugService")
public class UrlSlugService {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    public String toNovelSlug(Long id, String title) {
        return this.toSlug(id, title);
    }

    public String toChapterSlug(Long id, String title) {
        return this.toSlug(id, title);
    }

    public Long parseIdFromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Invalid slug");
        }
        int dashIndex = slug.indexOf('-');
        String idPart = dashIndex >= 0 ? slug.substring(0, dashIndex) : slug;
        if (idPart.isBlank() || !idPart.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid slug");
        }
        return Long.parseLong(idPart);
    }

    private String toSlug(Long id, String title) {
        String safeTitle = title == null ? "" : title;
        String normalized = Normalizer.normalize(safeTitle, Normalizer.Form.NFD);
        String noDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String lowered = noDiacritics.toLowerCase(Locale.ROOT);
        String dashed = NON_ALNUM.matcher(lowered).replaceAll("-");
        String compacted = MULTI_DASH.matcher(dashed).replaceAll("-");
        String cleanTitle = compacted.replaceAll("^-|-$", "");
        if (cleanTitle.isEmpty()) {
            return String.valueOf(id);
        }
        return id + "-" + cleanTitle;
    }
}