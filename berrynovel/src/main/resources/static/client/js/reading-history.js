/**
 * Browser-only reading history for chapter lists.
 * Stores chapter ids in localStorage under readChapters:<novelId>.
 */
(function (window, document) {
    "use strict";

    function getStorageKey(novelId) {
        if (novelId == null) return null;
        return "readChapters:" + String(novelId);
    }

    function getReadMap(storageKey) {
        if (!storageKey) return {};

        try {
            const storedValue = localStorage.getItem(storageKey);
            if (!storedValue) return {};

            const parsedValue = JSON.parse(storedValue);
            return parsedValue && typeof parsedValue === "object" && !Array.isArray(parsedValue)
                ? parsedValue
                : {};
        } catch (error) {
            return {};
        }
    }

    function saveReadMap(storageKey, readMap) {
        if (!storageKey) return;

        try {
            localStorage.setItem(storageKey, JSON.stringify(readMap));
        } catch (error) {
            // localStorage can be unavailable in private mode or when storage is full.
        }
    }

    function applyReadChapterStyles(novelId, rootSelector) {
        const storageKey = getStorageKey(novelId);
        const readMap = getReadMap(storageKey);
        const root = rootSelector ? document.querySelector(rootSelector) : document;

        if (!root) return;

        root.querySelectorAll("[data-chapter-id]").forEach(function (item) {
            const chapterId = item.dataset.chapterId;
            if (readMap[String(chapterId)]) {
                item.classList.add("chapter-read");
            } else {
                item.classList.remove("chapter-read");
            }
        });
    }

    function markCurrentChapterAsRead(novelId, chapterId, rootSelector) {
        if (novelId == null || chapterId == null) return;

        const storageKey = getStorageKey(novelId);
        const readMap = getReadMap(storageKey);
        readMap[String(chapterId)] = true;
        saveReadMap(storageKey, readMap);
        applyReadChapterStyles(novelId, rootSelector);
    }

    window.BerryReadingHistory = {
        applyReadChapterStyles: applyReadChapterStyles,
        markCurrentChapterAsRead: markCurrentChapterAsRead
    };
})(window, document);
