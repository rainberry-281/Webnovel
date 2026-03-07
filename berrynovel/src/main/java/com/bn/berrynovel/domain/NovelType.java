package com.bn.berrynovel.domain;

public enum NovelType {
    ORIGINAL("Original"),
    TRANSLATION("Translation");

    private final String displayName;

    NovelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
