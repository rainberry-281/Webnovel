package com.bn.berrynovel.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String noDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String lowered = noDiacritics
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT);
        String dashed = NON_ALNUM.matcher(lowered).replaceAll("-");
        String compacted = MULTI_DASH.matcher(dashed).replaceAll("-");

        return compacted.replaceAll("^-|-$", "");
    }
}
