package com.bn.berrynovel.domain;

public enum NovelHot {
    NOT_HOT("No"),
    HOT("Yes");

    private final String displayName;

    NovelHot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
