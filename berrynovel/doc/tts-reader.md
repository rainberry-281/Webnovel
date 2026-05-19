# Text-to-Speech (TTS) Integration — Implementation Spec

> **Project:** berrynovel (Spring Boot + Thymeleaf)
> **Feature:** Frontend-only TTS using Web Speech API
> **Page affected:** `reader/show.html` only
> **Date:** 2026-05-19

---

## 1. Current Reader Page Structure

Key points from `show.html`:

```html
<!-- Chapter title area (lines 98–102) -->
<div class="title-top">
    <h2 class="title-item text-xl font-bold text-center" th:text="${novel.title}"></h2>
    <h4 class="title-item text-base font-bold text-center" th:text="${chapter.title}"></h4>
</div>

<!-- Chapter content (line 106–108) -->
<div id="chapter-content" class="long-text text-justify" style="padding: 30px 0"
     th:utext="${chapter.content}">
</div>
```

Existing scripts already in `show.html`:
1. **Sidebar toggle** — `#rd-info-btn` click → `.rd_sidebar.on`
2. **Current chapter scroll** — `.rd_sidebar li.current` scrollIntoView
3. **Bookmark by paragraph** — `#bookmark-btn` click → fetch API (upsert/delete)
4. **Side icons toggle** — document click → `.rd-side-icons.show`

**Rule:** Do NOT touch any of these. TTS script is added independently.

---

## 2. Files to Create / Modify

| Action | File | Reason |
|---|---|---|
| **CREATE** | `static/client/js/tts-reader.js` | TTS logic — separate file |
| **MODIFY** | `templates/client/reader/show.html` | Add TTS panel HTML + `<script src>` tag |
| **MODIFY** | `static/client/css/chapter.css` | Add TTS panel styles |

**No backend changes.** No controller, service, repository, or entity changes.

---

## 3. HTML — TTS Control Panel

**Where to insert:** Inside `.reading-content`, between the `<div class="title-top">` block
and `<div id="chapter-content">`. Exact insertion after line 102 in `show.html`.

```html
<!-- ── TTS PANEL ─────────────────────────────────────────── -->
<div id="tts-panel" class="tts-panel" aria-label="Text to speech controls">

    <!-- Unsupported browser message (hidden by default, shown by JS) -->
    <p id="tts-unsupported" class="tts-unsupported" style="display:none;">
        Text-to-speech is not supported in this browser.
    </p>

    <!-- Controls (hidden if unsupported) -->
    <div id="tts-controls" class="tts-controls">

        <!-- Play / Pause / Resume / Stop buttons -->
        <button id="tts-play"   class="tts-button tts-button--primary" title="Listen">
            <i class="fas fa-play"></i> Listen
        </button>
        <button id="tts-pause"  class="tts-button" title="Pause"  disabled>
            <i class="fas fa-pause"></i> Pause
        </button>
        <button id="tts-resume" class="tts-button" title="Resume" disabled>
            <i class="fas fa-play-circle"></i> Resume
        </button>
        <button id="tts-stop"   class="tts-button" title="Stop"   disabled>
            <i class="fas fa-stop"></i> Stop
        </button>

        <!-- Divider -->
        <span class="tts-divider"></span>

        <!-- Voice selector -->
        <label for="tts-voice-select" class="tts-label">Voice</label>
        <select id="tts-voice-select" class="tts-select" title="Select voice">
            <option value="">Loading voices...</option>
        </select>

        <!-- Speed control -->
        <label for="tts-speed" class="tts-label">
            Speed: <span id="tts-speed-display">1.0×</span>
        </label>
        <input id="tts-speed" class="tts-speed" type="range"
               min="0.5" max="2" step="0.1" value="1"
               title="Reading speed">

        <!-- Progress indicator -->
        <span id="tts-progress" class="tts-progress"></span>
    </div>
</div>
<!-- ── END TTS PANEL ──────────────────────────────────────── -->
```

**Script tag** — add before `</body>`:

```html
<script th:src="@{/client/js/tts-reader.js}"></script>
```

> Add **after** existing inline `<script>` blocks but before `</body>`.

---

## 4. JavaScript — `static/client/js/tts-reader.js`

```javascript
/**
 * tts-reader.js
 * Text-to-Speech for the chapter reader using the Web Speech API.
 * Source text: #chapter-content
 * Controls:  #tts-play, #tts-pause, #tts-resume, #tts-stop
 *            #tts-voice-select, #tts-speed, #tts-speed-display
 *            #tts-progress
 */

(function () {
    "use strict";

    // ── Constants ────────────────────────────────────────────────────────────
    const CHUNK_SIZE     = 2000;   // max characters per utterance chunk
    const DEFAULT_RATE   = 1.0;
    const FALLBACK_LANG  = "vi-VN";

    // ── DOM refs (resolved after DOMContentLoaded) ───────────────────────────
    let playBtn, pauseBtn, resumeBtn, stopBtn;
    let voiceSelect, speedInput, speedDisplay, progressEl;
    let ttsPanel, ttsControls, ttsUnsupported;
    let chapterContent;

    // ── State ────────────────────────────────────────────────────────────────
    const state = {
        isSpeaking:        false,
        isPaused:          false,
        chunks:            [],
        currentChunkIndex: 0,
        currentVoice:      null,
        currentRate:       DEFAULT_RATE,
        currentHighlight:  null,   // highlighted <p> element
    };

    // ── Initialise ───────────────────────────────────────────────────────────
    document.addEventListener("DOMContentLoaded", function () {

        // Resolve DOM refs
        playBtn         = document.getElementById("tts-play");
        pauseBtn        = document.getElementById("tts-pause");
        resumeBtn       = document.getElementById("tts-resume");
        stopBtn         = document.getElementById("tts-stop");
        voiceSelect     = document.getElementById("tts-voice-select");
        speedInput      = document.getElementById("tts-speed");
        speedDisplay    = document.getElementById("tts-speed-display");
        progressEl      = document.getElementById("tts-progress");
        ttsPanel        = document.getElementById("tts-panel");
        ttsControls     = document.getElementById("tts-controls");
        ttsUnsupported  = document.getElementById("tts-unsupported");
        chapterContent  = document.getElementById("chapter-content");

        // Guard: required elements must exist
        if (!ttsPanel || !chapterContent) return;

        // Guard: browser support
        if (!("speechSynthesis" in window)) {
            if (ttsControls)    ttsControls.style.display  = "none";
            if (ttsUnsupported) ttsUnsupported.style.display = "";
            return;
        }

        // Load voices (may be async in Chrome/Edge)
        loadVoices();
        if (speechSynthesis.onvoiceschanged !== undefined) {
            speechSynthesis.onvoiceschanged = loadVoices;
        }

        // Bind controls
        playBtn.addEventListener("click",  onPlay);
        pauseBtn.addEventListener("click", onPause);
        resumeBtn.addEventListener("click", onResume);
        stopBtn.addEventListener("click",  onStop);

        voiceSelect.addEventListener("change", function () {
            const voices = speechSynthesis.getVoices();
            state.currentVoice = voices[voiceSelect.value] || null;
        });

        speedInput.addEventListener("input", function () {
            state.currentRate = parseFloat(speedInput.value);
            speedDisplay.textContent = state.currentRate.toFixed(1) + "×";
        });

        // Cleanup when leaving page
        window.addEventListener("beforeunload", function () {
            speechSynthesis.cancel();
        });
    });

    // ── Voice loading ────────────────────────────────────────────────────────

    function loadVoices() {
        const voices = speechSynthesis.getVoices();
        if (!voices || voices.length === 0) return;

        voiceSelect.innerHTML = "";

        // Prefer Vietnamese voices
        const viVoices  = voices.filter(v => v.lang && v.lang.startsWith("vi"));
        const allVoices = viVoices.length > 0 ? viVoices : voices;

        allVoices.forEach(function (voice, index) {
            // Find the original index in the full voice list (needed for lookup)
            const originalIndex = voices.indexOf(voice);
            const option = document.createElement("option");
            option.value       = originalIndex;
            option.textContent = voice.name + " (" + voice.lang + ")";
            voiceSelect.appendChild(option);
        });

        // Auto-select first available
        if (voiceSelect.options.length > 0) {
            voiceSelect.selectedIndex = 0;
            state.currentVoice = voices[parseInt(voiceSelect.value, 10)] || null;
        }
    }

    // ── Text extraction & chunking ───────────────────────────────────────────

    /**
     * Extract plain text from #chapter-content.
     * Respects paragraph structure: each <p> becomes a separate chunk candidate.
     */
    function extractChunks() {
        const paragraphs = chapterContent.querySelectorAll("p");

        let rawChunks = [];

        if (paragraphs.length > 0) {
            // Split by paragraph
            paragraphs.forEach(function (p) {
                const text = p.innerText || p.textContent || "";
                const cleaned = cleanText(text);
                if (cleaned.length > 0) {
                    rawChunks.push({ text: cleaned, element: p });
                }
            });
        } else {
            // Fallback: no <p> tags — use raw text split by CHUNK_SIZE
            const fullText = cleanText(chapterContent.innerText || chapterContent.textContent || "");
            for (let i = 0; i < fullText.length; i += CHUNK_SIZE) {
                rawChunks.push({ text: fullText.slice(i, i + CHUNK_SIZE), element: null });
            }
        }

        // Further split very long paragraphs
        const final = [];
        rawChunks.forEach(function (chunk) {
            if (chunk.text.length <= CHUNK_SIZE) {
                final.push(chunk);
            } else {
                // Long paragraph: split by CHUNK_SIZE, keep element ref only on first sub-chunk
                for (let i = 0; i < chunk.text.length; i += CHUNK_SIZE) {
                    final.push({
                        text:    chunk.text.slice(i, i + CHUNK_SIZE),
                        element: i === 0 ? chunk.element : null
                    });
                }
            }
        });

        return final;
    }

    function cleanText(text) {
        return (text || "")
            .replace(/\u00A0/g, " ")   // non-breaking spaces
            .replace(/\s+/g, " ")      // collapse whitespace
            .trim();
    }

    // ── Utterance factory ────────────────────────────────────────────────────

    function makeUtterance(text, onEnd, onError) {
        const utterance        = new SpeechSynthesisUtterance(text);
        utterance.rate         = state.currentRate;
        utterance.lang         = FALLBACK_LANG;

        if (state.currentVoice) {
            utterance.voice = state.currentVoice;
            utterance.lang  = state.currentVoice.lang || FALLBACK_LANG;
        }

        utterance.onend   = onEnd;
        utterance.onerror = function (e) {
            // "interrupted" fires when cancelled intentionally — ignore
            if (e.error !== "interrupted" && e.error !== "canceled") {
                console.warn("TTS error:", e.error);
            }
            if (onError) onError(e);
        };

        return utterance;
    }

    // ── Chunk reading loop ───────────────────────────────────────────────────

    function speakChunk(index) {
        if (index >= state.chunks.length) {
            // All chunks done
            onFinished();
            return;
        }

        state.currentChunkIndex = index;
        updateProgress(index, state.chunks.length);
        highlightChunk(index);

        const chunk     = state.chunks[index];
        const utterance = makeUtterance(
            chunk.text,
            function () {
                // Auto-advance to next chunk
                if (state.isSpeaking && !state.isPaused) {
                    speakChunk(index + 1);
                }
            },
            function (e) {
                if (e.error !== "interrupted" && e.error !== "canceled") {
                    // Try to continue on non-fatal errors
                    if (state.isSpeaking) {
                        speakChunk(index + 1);
                    }
                }
            }
        );

        speechSynthesis.speak(utterance);
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    function onPlay() {
        // If already speaking, cancel then restart from beginning
        if (state.isSpeaking || state.isPaused) {
            speechSynthesis.cancel();
            clearHighlight();
        }

        const content = chapterContent.innerText || chapterContent.textContent || "";
        if (!content.trim()) {
            updateProgress(0, 0, "No content to read.");
            return;
        }

        state.chunks            = extractChunks();
        state.currentChunkIndex = 0;
        state.isSpeaking        = true;
        state.isPaused          = false;

        updateButtons();
        speakChunk(0);
    }

    function onPause() {
        if (!state.isSpeaking || state.isPaused) return;
        speechSynthesis.pause();
        state.isPaused = true;
        updateButtons();
    }

    function onResume() {
        if (!state.isPaused) return;
        speechSynthesis.resume();
        state.isPaused = false;
        updateButtons();
    }

    function onStop() {
        speechSynthesis.cancel();
        state.isSpeaking        = false;
        state.isPaused          = false;
        state.currentChunkIndex = 0;
        state.chunks            = [];
        clearHighlight();
        updateButtons();
        updateProgress(0, 0, "");
    }

    function onFinished() {
        state.isSpeaking        = false;
        state.isPaused          = false;
        state.currentChunkIndex = 0;
        clearHighlight();
        updateButtons();
        updateProgress(0, 0, "Finished.");
        // Clear "Finished" message after 3 s
        setTimeout(function () { updateProgress(0, 0, ""); }, 3000);
    }

    // ── UI helpers ───────────────────────────────────────────────────────────

    function updateButtons() {
        const speaking = state.isSpeaking;
        const paused   = state.isPaused;

        playBtn.disabled   = false;                   // always available
        pauseBtn.disabled  = !speaking || paused;
        resumeBtn.disabled = !paused;
        stopBtn.disabled   = !speaking && !paused;

        // Visual feedback on play button
        playBtn.innerHTML = speaking && !paused
            ? '<i class="fas fa-redo"></i> Restart'
            : '<i class="fas fa-play"></i> Listen';
    }

    function updateProgress(current, total, message) {
        if (!progressEl) return;
        if (message !== undefined && message !== null) {
            progressEl.textContent = message;
            return;
        }
        if (total > 0) {
            progressEl.textContent = "Part " + (current + 1) + " / " + total;
        } else {
            progressEl.textContent = "";
        }
    }

    // ── Highlight helpers ────────────────────────────────────────────────────

    function highlightChunk(index) {
        clearHighlight();
        const chunk = state.chunks[index];
        if (chunk && chunk.element) {
            chunk.element.classList.add("tts-reading");
            state.currentHighlight = chunk.element;
            // Scroll paragraph into view smoothly
            chunk.element.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }

    function clearHighlight() {
        if (state.currentHighlight) {
            state.currentHighlight.classList.remove("tts-reading");
            state.currentHighlight = null;
        }
        // Fallback: clear all in case of stale state
        document.querySelectorAll(".tts-reading").forEach(function (el) {
            el.classList.remove("tts-reading");
        });
    }

})();
```

---

## 5. CSS — `static/client/css/chapter.css` (append at end)

```css
/* ── TTS Panel ──────────────────────────────────────────────────────────── */

.tts-panel {
    margin: 16px 0 24px;
    padding: 12px 16px;
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 10px;
    font-size: 0.88rem;
}

.tts-unsupported {
    color: #6c757d;
    margin: 0;
    font-style: italic;
}

.tts-controls {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
}

/* Buttons */
.tts-button {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 5px 12px;
    border: 1px solid #ced4da;
    border-radius: 6px;
    background: #fff;
    color: #333;
    font-size: 0.82rem;
    cursor: pointer;
    transition: background 0.18s, color 0.18s, border-color 0.18s;
    white-space: nowrap;
}

.tts-button:hover:not(:disabled) {
    background: #e9ecef;
    border-color: #adb5bd;
}

.tts-button:disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

.tts-button--primary {
    background: #495057;
    color: #fff;
    border-color: #495057;
}

.tts-button--primary:hover:not(:disabled) {
    background: #343a40;
    border-color: #343a40;
}

/* Divider */
.tts-divider {
    display: inline-block;
    width: 1px;
    height: 24px;
    background: #dee2e6;
    margin: 0 4px;
}

/* Label */
.tts-label {
    font-size: 0.82rem;
    color: #6c757d;
    margin: 0;
    white-space: nowrap;
}

/* Voice select */
.tts-select {
    padding: 4px 8px;
    border: 1px solid #ced4da;
    border-radius: 6px;
    background: #fff;
    font-size: 0.82rem;
    max-width: 200px;
    cursor: pointer;
}

/* Speed range */
.tts-speed {
    width: 90px;
    cursor: pointer;
    accent-color: #495057;
}

/* Progress */
.tts-progress {
    font-size: 0.78rem;
    color: #6c757d;
    white-space: nowrap;
}

/* Highlighted paragraph being read */
.tts-reading {
    background: rgba(255, 243, 128, 0.35);
    border-left: 3px solid #f0c040;
    padding-left: 6px;
    transition: background 0.25s;
    border-radius: 2px;
}

/* Dark mode compatibility (if project ever adds dark mode) */
@media (prefers-color-scheme: dark) {
    .tts-panel {
        background: #2a2a2a;
        border-color: #444;
    }
    .tts-button {
        background: #333;
        color: #eee;
        border-color: #555;
    }
    .tts-button:hover:not(:disabled) {
        background: #3a3a3a;
    }
    .tts-button--primary {
        background: #555;
        border-color: #555;
    }
    .tts-select {
        background: #333;
        color: #eee;
        border-color: #555;
    }
    .tts-reading {
        background: rgba(255, 220, 50, 0.15);
        border-left-color: #c8a000;
    }
}
```

---

## 6. Full Diff for `show.html`

Only two additions, nothing removed:

### Addition 1 — TTS Panel HTML (after line 102, before `chapter-content`)

```diff
         <div class="title-top" style="padding-top: 20px;">
             <h2 class="title-item text-xl font-bold text-center" th:text="${novel.title}"></h2>
             <h4 class="title-item text-base font-bold text-center" th:text="${chapter.title}"></h4>
         </div>

+        <!-- ── TTS PANEL ───────────────────────────────────── -->
+        <div id="tts-panel" class="tts-panel" aria-label="Text to speech controls">
+            <p id="tts-unsupported" class="tts-unsupported" style="display:none;">
+                Text-to-speech is not supported in this browser.
+            </p>
+            <div id="tts-controls" class="tts-controls">
+                <button id="tts-play"   class="tts-button tts-button--primary" title="Listen">
+                    <i class="fas fa-play"></i> Listen
+                </button>
+                <button id="tts-pause"  class="tts-button" title="Pause"  disabled>
+                    <i class="fas fa-pause"></i> Pause
+                </button>
+                <button id="tts-resume" class="tts-button" title="Resume" disabled>
+                    <i class="fas fa-play-circle"></i> Resume
+                </button>
+                <button id="tts-stop"   class="tts-button" title="Stop"   disabled>
+                    <i class="fas fa-stop"></i> Stop
+                </button>
+                <span class="tts-divider"></span>
+                <label for="tts-voice-select" class="tts-label">Voice</label>
+                <select id="tts-voice-select" class="tts-select" title="Select voice">
+                    <option value="">Loading voices...</option>
+                </select>
+                <label for="tts-speed" class="tts-label">
+                    Speed: <span id="tts-speed-display">1.0×</span>
+                </label>
+                <input id="tts-speed" class="tts-speed" type="range"
+                       min="0.5" max="2" step="0.1" value="1" title="Reading speed">
+                <span id="tts-progress" class="tts-progress"></span>
+            </div>
+        </div>
+        <!-- ── END TTS PANEL ───────────────────────────────── -->

         <div style="text-align: center; margin: 20px auto -20px auto;"></div>

         <div id="chapter-content" class="long-text text-justify" ...>
```

### Addition 2 — Script tag (before `</body>`)

```diff
     </script>

+    <script th:src="@{/client/js/tts-reader.js}"></script>
 </body>
```

---

## 7. Full Logic Flow Diagram

```
DOMContentLoaded
    │
    ├─ speechSynthesis not supported?
    │   └─ show "not supported" message, hide controls → DONE
    │
    ├─ Load voices (getVoices + onvoiceschanged)
    │   ├─ filter for lang starting "vi"
    │   │   → found: populate select with Vietnamese voices
    │   └─ none found: populate select with all voices
    │
    └─ Bind event listeners

User clicks [Listen]
    │
    ├─ Already speaking/paused? → cancel() + clearHighlight
    ├─ Extract text from #chapter-content
    │   ├─ <p> tags found → one chunk per paragraph (+ split >2000 chars)
    │   └─ no <p> tags → split raw text every 2000 chars
    ├─ state.chunks = [chunk0, chunk1, ...]
    ├─ updateButtons()
    └─ speakChunk(0)
            │
            ├─ highlightChunk(0) → p.classList.add("tts-reading"), scrollIntoView
            ├─ new SpeechSynthesisUtterance(chunk0.text)
            │   utterance.rate = currentRate
            │   utterance.voice = currentVoice (or fallback lang "vi-VN")
            ├─ utterance.onend → speakChunk(1) → ... → speakChunk(N)
            └─ speechSynthesis.speak(utterance)

User clicks [Pause]  → speechSynthesis.pause(), isPaused=true, updateButtons()
User clicks [Resume] → speechSynthesis.resume(), isPaused=false, updateButtons()
User clicks [Stop]   → speechSynthesis.cancel(), reset state, clearHighlight()

All chunks done → onFinished() → show "Finished.", clear after 3s
Page unload      → speechSynthesis.cancel()
```

---

## 8. Edge Case Handling

| Case | Handling |
|---|---|
| Browser has no Web Speech API | Controls hidden, unsupported message shown |
| Empty chapter content | `onPlay()` shows "No content to read." in progress area |
| No voices loaded yet | `voiceSelect` shows "Loading voices..." — voices fill in via `onvoiceschanged` |
| No Vietnamese voice | All voices shown; `utterance.lang = "vi-VN"` set as fallback |
| User clicks Listen while playing | `cancel()` + restart from chunk 0 |
| User clicks Stop while paused | `cancel()` resets state correctly |
| Very long chapter | Text split into ≤2000 char chunks; read sequentially |
| `chapter-content` has only divs, no `<p>` | Falls back to raw text split by CHUNK_SIZE |
| Voice list loads late (Chrome async) | `onvoiceschanged` callback re-populates select |
| User changes speed mid-speech | `currentRate` updated; applies on next utterance (next chunk) |
| User changes voice mid-speech | `currentVoice` updated; applies on next utterance (next chunk) |
| Non-fatal utterance error | Logs warning, continues to next chunk |
| Fatal/interrupted error | Recognised as intentional cancel — silently ignored |
| Page refresh while speaking | `beforeunload` → `speechSynthesis.cancel()` |

---

## 9. Test Instructions

### Manual test steps

1. Open any chapter page in Chrome/Edge/Firefox
2. Verify TTS panel appears between chapter title and content
3. Click **Listen** → speech starts, first paragraph highlighted
4. Click **Pause** → speech pauses, Pause button disabled, Resume enabled
5. Click **Resume** → speech continues from where it stopped
6. Click **Stop** → speech stops, all buttons reset, highlight cleared
7. Change **Voice** dropdown → next chunk uses new voice
8. Change **Speed** slider → speed display updates; next chunk uses new speed
9. Click **Listen** while playing → restarts from beginning
10. Navigate to another chapter while speaking → speech stops (beforeunload)
11. Open in a browser without Web Speech API (e.g., older IE/Safari) → unsupported message shown

### Verify existing features still work

- Sidebar info button still opens chapter list
- Bookmark button still saves/removes bookmark with paragraph position
- Side icons still toggle on click
- Read count still increments (backend unaffected)

---

## 10. Browser Compatibility Notes

| Browser | Web Speech API | Notes |
|---|---|---|
| Chrome 33+ | ✅ Full support | Voices load asynchronously — handled |
| Edge 14+ | ✅ Full support | Same as Chrome |
| Firefox 49+ | ✅ Supported | Fewer voices than Chrome |
| Safari 14.1+ | ✅ Supported | Good Vietnamese voice support on macOS |
| Opera | ✅ Supported | |
| IE/legacy | ❌ None | Unsupported message shown |
| Mobile Chrome | ✅ Supported | Voice list may vary by OS language |
| Mobile Safari | ✅ Supported | Requires user gesture (already satisfied by button click) |

---

## 11. Summary: Files Changed

| File | Action | Description |
|---|---|---|
| `static/client/js/tts-reader.js` | **CREATE** | Full TTS logic in IIFE — ~200 lines |
| `templates/client/reader/show.html` | **MODIFY** | Add `<div id="tts-panel">` after title-top block; add `<script th:src>` before `</body>` |
| `static/client/css/chapter.css` | **MODIFY** | Append TTS panel + button + highlight styles (~90 lines) |

### NOT changed

- All existing inline scripts (sidebar, bookmark, scroll, side-icons)
- Any backend Java file
- Any route, controller, service, or repository
- Database schema
- Bookmark logic
- Read count logic
- Hot tag logic
- Authentication
