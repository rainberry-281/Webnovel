/**
 * Floating text-to-speech reader for chapter pages.
 * Uses the browser Web Speech API only.
 */
(function () {
    "use strict";

    const CHUNK_SIZE = 2000;
    const DEFAULT_RATE = 1.0;
    const FALLBACK_LANG = "vi-VN";

    let toggleBtn;
    let closeBtn;
    let panel;
    let controls;
    let unsupportedMessage;
    let playBtn;
    let pauseBtn;
    let resumeBtn;
    let stopBtn;
    let voiceSelect;
    let rateInput;
    let rateValue;
    let progressEl;
    let chapterContent;
    let isSupported = false;

    const state = {
        isSpeaking: false,
        isPaused: false,
        chunks: [],
        currentChunkIndex: 0,
        currentVoice: null,
        currentRate: DEFAULT_RATE,
        currentHighlight: null
    };

    document.addEventListener("DOMContentLoaded", function () {
        toggleBtn = document.getElementById("tts-toggle-btn");
        closeBtn = document.getElementById("tts-close-btn");
        panel = document.getElementById("tts-reader-panel");
        controls = document.getElementById("tts-reader-controls");
        unsupportedMessage = document.getElementById("tts-unsupported");
        playBtn = document.getElementById("tts-play-btn");
        pauseBtn = document.getElementById("tts-pause-btn");
        resumeBtn = document.getElementById("tts-resume-btn");
        stopBtn = document.getElementById("tts-stop-btn");
        voiceSelect = document.getElementById("tts-voice-select");
        rateInput = document.getElementById("tts-rate-input");
        rateValue = document.getElementById("tts-rate-value");
        progressEl = document.getElementById("tts-progress");
        chapterContent = document.getElementById("chapter-content");

        if (!toggleBtn || !panel || !chapterContent) return;

        isSupported = "speechSynthesis" in window && "SpeechSynthesisUtterance" in window;

        toggleBtn.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            togglePanel();
        });

        panel.addEventListener("click", function (event) {
            event.stopPropagation();
        });

        if (closeBtn) {
            closeBtn.addEventListener("click", function (event) {
                event.preventDefault();
                event.stopPropagation();
                hidePanel();
            });
        }

        bindControlStopPropagation();

        if (!isSupported) {
            showUnsupportedState();
            return;
        }

        loadVoices();
        if (speechSynthesis.onvoiceschanged !== undefined) {
            speechSynthesis.onvoiceschanged = loadVoices;
        }

        playBtn.addEventListener("click", onPlay);
        pauseBtn.addEventListener("click", onPause);
        resumeBtn.addEventListener("click", onResume);
        stopBtn.addEventListener("click", onStop);

        voiceSelect.addEventListener("change", function () {
            const voices = speechSynthesis.getVoices();
            state.currentVoice = voices[voiceSelect.value] || null;
        });

        rateInput.addEventListener("input", function () {
            state.currentRate = parseFloat(rateInput.value);
            rateValue.textContent = state.currentRate.toFixed(1) + "x";
        });

        window.addEventListener("beforeunload", function () {
            speechSynthesis.cancel();
        });
    });

    function bindControlStopPropagation() {
        [playBtn, pauseBtn, resumeBtn, stopBtn, voiceSelect, rateInput].forEach(function (element) {
            if (!element) return;
            element.addEventListener("click", function (event) {
                event.stopPropagation();
            });
        });
    }

    function togglePanel() {
        if (panel.classList.contains("open")) {
            hidePanel();
            return;
        }
        showPanel();
    }

    function showPanel() {
        panel.classList.add("open");
        panel.setAttribute("aria-hidden", "false");

        if (!isSupported) {
            showUnsupportedState();
        }
    }

    function hidePanel() {
        panel.classList.remove("open");
        panel.setAttribute("aria-hidden", "true");
    }

    function showUnsupportedState() {
        if (unsupportedMessage) unsupportedMessage.style.display = "";
        if (voiceSelect) {
            voiceSelect.innerHTML = '<option value="">No browser voices available</option>';
            voiceSelect.disabled = true;
        }
        if (rateInput) rateInput.disabled = true;
        [playBtn, pauseBtn, resumeBtn, stopBtn].forEach(function (button) {
            if (button) button.disabled = true;
        });
        updateProgress("Text-to-speech is not supported in this browser.");
    }

    function loadVoices() {
        const voices = speechSynthesis.getVoices();
        if (!voiceSelect || !voices || voices.length === 0) return;

        const previousValue = voiceSelect.value;
        voiceSelect.innerHTML = "";
        voiceSelect.disabled = false;

        const viVoices = voices.filter(function (voice) {
            return voice.lang && voice.lang.toLowerCase().startsWith("vi");
        });
        const selectableVoices = viVoices.length > 0 ? viVoices : voices;

        selectableVoices.forEach(function (voice) {
            const originalIndex = voices.indexOf(voice);
            const option = document.createElement("option");
            option.value = String(originalIndex);
            option.textContent = voice.name + " (" + voice.lang + ")";
            voiceSelect.appendChild(option);
        });

        if (previousValue && voices[previousValue]) {
            voiceSelect.value = previousValue;
        } else if (voiceSelect.options.length > 0) {
            voiceSelect.selectedIndex = 0;
        }

        state.currentVoice = voices[voiceSelect.value] || null;
    }

    function extractChunks() {
        const paragraphs = Array.from(chapterContent.querySelectorAll("p, div.paragraph"));
        const rawChunks = [];

        if (paragraphs.length > 0) {
            paragraphs.forEach(function (paragraph) {
                const text = cleanText(paragraph.innerText || paragraph.textContent || "");
                if (text.length > 0) {
                    rawChunks.push({ text: text, element: paragraph });
                }
            });
        } else {
            const fullText = cleanText(chapterContent.innerText || chapterContent.textContent || "");
            for (let i = 0; i < fullText.length; i += CHUNK_SIZE) {
                rawChunks.push({ text: fullText.slice(i, i + CHUNK_SIZE), element: null });
            }
        }

        const chunks = [];
        rawChunks.forEach(function (chunk) {
            if (chunk.text.length <= CHUNK_SIZE) {
                chunks.push(chunk);
                return;
            }

            splitLongText(chunk.text).forEach(function (text, index) {
                chunks.push({
                    text: text,
                    element: index === 0 ? chunk.element : null
                });
            });
        });

        return chunks;
    }

    function splitLongText(text) {
        const chunks = [];
        let remaining = text;

        while (remaining.length > CHUNK_SIZE) {
            let splitAt = remaining.lastIndexOf(".", CHUNK_SIZE);
            if (splitAt < CHUNK_SIZE * 0.5) {
                splitAt = remaining.lastIndexOf(" ", CHUNK_SIZE);
            }
            if (splitAt < CHUNK_SIZE * 0.5) {
                splitAt = CHUNK_SIZE;
            }

            const splitEnd = splitAt >= CHUNK_SIZE ? splitAt : splitAt + 1;
            chunks.push(remaining.slice(0, splitEnd).trim());
            remaining = remaining.slice(splitEnd).trim();
        }

        if (remaining.length > 0) {
            chunks.push(remaining);
        }

        return chunks;
    }

    function cleanText(text) {
        return (text || "")
            .replace(/\u00A0/g, " ")
            .replace(/\s+/g, " ")
            .trim();
    }

    function makeUtterance(text, onEnd, onError) {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = state.currentRate;
        utterance.lang = FALLBACK_LANG;

        if (state.currentVoice) {
            utterance.voice = state.currentVoice;
            utterance.lang = state.currentVoice.lang || FALLBACK_LANG;
        }

        utterance.onend = onEnd;
        utterance.onerror = function (event) {
            if (event.error !== "interrupted" && event.error !== "canceled") {
                console.warn("TTS error:", event.error);
            }
            if (onError) onError(event);
        };

        return utterance;
    }

    function speakChunk(index) {
        if (!state.isSpeaking) return;

        if (index >= state.chunks.length) {
            onFinished();
            return;
        }

        state.currentChunkIndex = index;
        updateProgress("Part " + (index + 1) + " / " + state.chunks.length);
        highlightChunk(index);

        const chunk = state.chunks[index];
        const utterance = makeUtterance(
            chunk.text,
            function () {
                if (state.isSpeaking && !state.isPaused) {
                    speakChunk(index + 1);
                }
            },
            function (event) {
                if (event.error !== "interrupted" && event.error !== "canceled" && state.isSpeaking) {
                    speakChunk(index + 1);
                }
            }
        );

        speechSynthesis.speak(utterance);
    }

    function onPlay(event) {
        event.stopPropagation();
        state.isSpeaking = false;
        state.isPaused = false;
        speechSynthesis.cancel();
        clearHighlight();

        const content = chapterContent.innerText || chapterContent.textContent || "";
        if (!content.trim()) {
            updateProgress("No content to read.");
            return;
        }

        state.chunks = extractChunks();
        state.currentChunkIndex = 0;
        state.isSpeaking = true;
        state.isPaused = false;

        updateButtons();
        speakChunk(0);
    }

    function onPause(event) {
        event.stopPropagation();
        if (!state.isSpeaking || state.isPaused) return;

        speechSynthesis.pause();
        state.isPaused = true;
        updateButtons();
    }

    function onResume(event) {
        event.stopPropagation();
        if (!state.isPaused) return;

        speechSynthesis.resume();
        state.isPaused = false;
        updateButtons();
    }

    function onStop(event) {
        event.stopPropagation();
        speechSynthesis.cancel();
        resetState();
        updateButtons();
        updateProgress("");
    }

    function onFinished() {
        resetState();
        updateButtons();
        updateProgress("Finished.");
        setTimeout(function () {
            updateProgress("");
        }, 3000);
    }

    function resetState() {
        state.isSpeaking = false;
        state.isPaused = false;
        state.currentChunkIndex = 0;
        state.chunks = [];
        clearHighlight();
    }

    function updateButtons() {
        const speaking = state.isSpeaking;
        const paused = state.isPaused;

        playBtn.disabled = false;
        pauseBtn.disabled = !speaking || paused;
        resumeBtn.disabled = !paused;
        stopBtn.disabled = !speaking && !paused;

        playBtn.innerHTML = speaking && !paused
            ? '<i class="fas fa-redo"></i> Restart'
            : '<i class="fas fa-play"></i> Listen';
    }

    function updateProgress(message) {
        if (progressEl) {
            progressEl.textContent = message || "";
        }
    }

    function highlightChunk(index) {
        clearHighlight();
        const chunk = state.chunks[index];
        if (chunk && chunk.element) {
            chunk.element.classList.add("tts-reading");
            state.currentHighlight = chunk.element;
            chunk.element.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }

    function clearHighlight() {
        if (state.currentHighlight) {
            state.currentHighlight.classList.remove("tts-reading");
            state.currentHighlight = null;
        }

        document.querySelectorAll(".tts-reading").forEach(function (element) {
            element.classList.remove("tts-reading");
        });
    }
})();
