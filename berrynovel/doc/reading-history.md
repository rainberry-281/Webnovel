# Reading History Feature — Documentation

## Overview

Browser-based reading history for chapter pages.

When a user opens a chapter, the browser automatically marks that chapter as **read** using `localStorage`.
In the sidebar chapter list, read chapters get a **gray background** to distinguish them from unread ones.

This feature:
- Requires **no backend changes**
- Requires **no login**
- Uses **only `localStorage`**
- Is **scoped per novel** (novel A's history does not affect novel B)

---

## Files Changed

| File | Type | Change |
|---|---|---|
| `src/main/resources/templates/client/reader/show.html` | Thymeleaf template | Add `data-chapter-id` to `<li>`, add inline script block |
| `src/main/resources/static/client/css/chapter.css` | CSS | Add `.chapter-read` styles |

No Java/backend files are modified.

---

## How It Works

### 1. localStorage Key Format

History is stored per novel using a namespaced key:

```
readChapters:<novelId>
```

**Example:**
```
Key:   readChapters:42
Value: {"1": true, "7": true, "8": true}
```

Each key in the value object is a **chapter ID as a string**.
The value is always `true`.

### 2. Mark Chapter as Read

On `DOMContentLoaded`, the script:

1. Reads `novelId` and `currentChapterId` from Thymeleaf inline variables
2. Reads the existing read map from `localStorage` (or starts with `{}`)
3. Adds `currentChapterId` to the map
4. Saves the updated map back to `localStorage`

```js
const storageKey = `readChapters:${novelId}`;
const readMap = JSON.parse(localStorage.getItem(storageKey) || "{}");
readMap[String(currentChapterId)] = true;
localStorage.setItem(storageKey, JSON.stringify(readMap));
```

### 3. Apply Gray Style to Read Chapters

After saving, the script loops through all sidebar `<li>` elements that have a `data-chapter-id` attribute.
If the chapter ID is in `readMap`, the class `chapter-read` is added to that `<li>`.

```js
document.querySelectorAll(".sub-chap_list li[data-chapter-id]").forEach(item => {
    const chapId = item.dataset.chapterId;
    if (readMap[String(chapId)]) {
        item.classList.add("chapter-read");
    }
});
```

### 4. CSS Styling

```css
/* Read chapter — gray background */
.sub-chap_list li.chapter-read {
    background-color: #e8e8e8;
    border-radius: 4px;
}

.sub-chap_list li.chapter-read > a {
    color: #888;
}

/* Current chapter always wins — more specific selector */
.sub-chap_list li.current.chapter-read {
    background-color: #ddd;  /* same as .current */
}

.sub-chap_list li.current.chapter-read > a {
    color: inherit;
}
```

The existing `.rd_sidebar li.current` rule uses `background-color: #ddd; font-weight: 700;`.
The `.current.chapter-read` override keeps `current` dominant.

### 5. Thymeleaf Inline Variables

```html
<script th:inline="javascript">
    const novelId = /*[[${novel.id}]]*/ null;
    const currentChapterId = /*[[${chapter.id}]]*/ null;
</script>
```

Both values are rendered as numbers by Thymeleaf (Java `Long`).
The script converts them to strings with `String(...)` before using as object keys.

### 6. Template Change — `data-chapter-id` Attribute

```html
<li th:each="chap : ${chapters}"
    th:attr="data-chapter-id=${chap.id}"
    th:classappend="${chap.id == chapter.id} ? ' current'">
```

`th:attr` adds `data-chapter-id` to each list item.

---

## Thymeleaf Variables Available

From `ReaderController.java`:

| Variable | Type | Description |
|---|---|---|
| `${novel.id}` | `Long` | Current novel's ID |
| `${novel.title}` | `String` | Novel title |
| `${chapter.id}` | `Long` | Current chapter's ID |
| `${chapter.title}` | `String` | Current chapter title |
| `${chapters}` | `List<Chapter>` | Full chapter list for sidebar |
| `${prev}` | `Chapter` (nullable) | Previous chapter |
| `${next}` | `Chapter` (nullable) | Next chapter |
| `${isBookmarked}` | `boolean` | Whether user has bookmarked this chapter |
| `${savedLinePosition}` | `Integer` (nullable) | Bookmarked line position |
| `${savedParagraphKey}` | `String` (nullable) | Bookmarked paragraph key |

---

## Existing JavaScript (Do Not Break)

### Sidebar toggle (`show.html` inline script 1)
- `openSidebar()`, `closeSidebar()`, `toggleSidebar()`
- Bound to `#rd-info-btn` click and `.black-click` overlay click
- `Escape` key closes sidebar

### Current chapter scroll (`show.html` inline script 2)
- Scrolls `.rd_sidebar li.current` into view with `scrollIntoView({ block: "center" })`

### Bookmark + line position (`show.html` inline script 3 — `th:inline="javascript"`)
- Reads `savedParagraphKey`, `savedLinePosition`, `isBookmarked` from Thymeleaf
- Prepares paragraphs with `data-key` attributes
- Renders bookmark indicator (`#bm-indicator-icon`)
- Scrolls to saved position if `?from=bookmark`
- Handles `#bookmark-btn` click (async POST to `/bookshelf/bookmark/upsert` and `/bookshelf/bookmark/delete`)

### Side icons toggle (`show.html` inline script 4)
- Toggles `.show` class on `.rd-side-icons` on any body click
- Ignores clicks inside `.rd_sidebar`

### TTS Reader (`/client/js/tts-reader.js`)
- IIFE module, independent
- Uses `#tts-toggle-btn`, `#tts-close-btn`, `#tts-reader-panel`, `#tts-reader-controls`
- Reads `#chapter-content` for speech
- Adds/removes `.tts-reading` class on paragraphs

---

## Edge Cases Handled

| Scenario | Handling |
|---|---|
| `localStorage` unavailable (private mode, storage full) | Wrapped in `try/catch`, silently does nothing |
| Invalid JSON in stored value | Defaults to `{}` on parse error |
| `novelId` is `null` | Early return, nothing is saved |
| `chapterId` is `null` | Early return, nothing is saved |
| No sidebar `<li>` elements present | `querySelectorAll` returns empty list, no error |
| Chapter ID stored as number vs string | All IDs coerced with `String(...)` before key lookup |
| User clears browser storage | On next chapter open, history starts fresh from `{}` |

---

## Limitations

- History is **per-browser, per-device** only — not synced across devices or accounts
- Works for **anonymous and logged-in users** equally
- If user clears browser data, all reading history is lost
- No server-side backup of reading history

---

## Why No Database Changes

The feature is implemented entirely in the browser's `localStorage`.
No HTTP requests are made, no server state is changed, and no entity classes or controllers are modified.
The backend already provides `novel.id` and `chapter.id` in the Thymeleaf model, which are the only values needed.
