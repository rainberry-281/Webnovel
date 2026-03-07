package com.bn.berrynovel.domain;

public enum NovelProgress {
    ONGOING("Ongoing"),
    PAUSED("Paused"),
    COMPLETED("Completed");

    private final String displayName;

    NovelProgress(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
