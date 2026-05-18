# Kịch bản nâng cấp Bookmark: Chapter-level → Line Position

> **Dự án:** berrynovel (Spring Boot + Thymeleaf + MySQL)  
> **Ngày:** 2026-05-18  
> **Phiên bản hiện tại phân tích từ:** `src/main/java/com/bn/berrynovel/`

---

## 1. Phân tích hiện trạng

### 1.1 Entity `Bookmark.java`

```
Bookmark {
    id          : Long (PK, AUTO_INCREMENT)
    user        : User  (FK user_id)
    novel       : Novel (FK novel_id)
    chapter     : Chapter (FK chapter_id)
    createdAt   : LocalDateTime
}
UNIQUE KEY: uk_bookmark_user_chapter (user_id, chapter_id)
```

**Nhận xét:** Constraint `UNIQUE(user_id, chapter_id)` đã tồn tại → đúng hướng, chỉ cần thêm cột `line_position`.

---

### 1.2 Repository `BookmarkRepository.java`

```java
boolean existsByUser_IdAndChapter_Id(Long userId, Long chapterId);
void    deleteByUser_IdAndChapter_Id(Long userId, Long chapterId);
List<Bookmark> findByUser_IdOrderByCreatedAtDesc(Long userId);
```

Hiện chưa có phương thức `findByUser_IdAndChapter_Id` để **lấy một bookmark cụ thể** nhằm cập nhật `linePosition`.

---

### 1.3 Service `LibraryService.java` — phương thức liên quan

| Phương thức | Hành vi hiện tại |
|---|---|
| `toggleChapterBookmark(username, novelId, chapterId)` | Toggle: nếu đã có → xóa; chưa có → tạo mới |
| `isChapterBookmarked(username, chapterId)` | Kiểm tra tồn tại |
| `getBookmarkItems(username)` | Trả danh sách `BookmarkNovelDTO` (Novel + List\<Chapter\>) |

**Vấn đề:** `toggleChapterBookmark` chỉ toggle, không nhận `linePosition`. Cần tách thành **upsert** riêng.

---

### 1.4 Controller `ClientLibraryController.java`

```
POST /bookshelf/bookmark/toggle/{novelId}/{chapterId}
→ gọi toggleChapterBookmark
→ redirect:/reader/{novelId}/{chapterId}
```

Hiện submit bằng HTML form (không phải AJAX). Để truyền `linePosition`, cần **thêm hidden input** hoặc chuyển sang AJAX.

---

### 1.5 Template `reader/show.html`

- Nội dung chương nằm trong: `<div id="chapter-content" class="long-text">` (render bằng `th:utext`)
- Form bookmark: `<form class="bookmark-toggle-form">` → POST thông thường
- Chưa có logic scroll hay lưu/khôi phục vị trí

---

### 1.6 Template `library/bookmark.html`

Link mở bookmark:
```html
<a th:href="@{/reader/{novelId}/{chapterId}(novelId=..., chapterId=...)}">
```
→ Mở chapter từ **đầu trang**, chưa có `linePosition` trong URL hay data attribute.

---

## 2. Kiến trúc thay đổi đề xuất

```
[User đọc tới dòng X]
        │
        ▼
[Click nút Bookmark]
        │
        ▼ (JavaScript đọc scroll position / paragraph index)
[POST /bookshelf/bookmark/upsert/{novelId}/{chapterId}]
   body: { linePosition: X, paragraphKey: "p-42" }
        │
        ▼
[LibraryService.upsertChapterBookmark()]
   → findByUser_IdAndChapter_Id → nếu có → UPDATE linePosition
                                → nếu chưa → INSERT mới
        │
        ▼
[DB: Bookmark table với cột line_position mới]

─────────────────────────────────────────

[User mở trang bookmark.html]
        │
        ▼ link có data-line-position="X" hoặc ?line=X
[reader/show.html load]
        │
        ▼ (JavaScript)
[scrollToSavedPosition(linePosition)]
   → tìm paragraph theo key → scrollIntoView
   → fallback: window.scrollTo(0, 0)
```

---

## 3. Thay đổi Schema Database

### 3.1 Migration SQL (thêm cột)

```sql
-- V2__add_line_position_to_bookmark.sql
ALTER TABLE Bookmark
    ADD COLUMN line_position  INT          NULL COMMENT 'Vị trí dòng/paragraph đã đọc',
    ADD COLUMN paragraph_key  VARCHAR(255) NULL COMMENT 'data-key của paragraph (stable anchor)',
    ADD COLUMN updated_at     DATETIME     NULL COMMENT 'Lần cập nhật cuối';
```

> **Lưu ý:** Constraint `UNIQUE(user_id, chapter_id)` **giữ nguyên** — đây là nền tảng của logic upsert.

### 3.2 Schema sau thay đổi

```
Bookmark {
    id            : BIGINT PK AUTO_INCREMENT
    user_id       : BIGINT FK → Users.id
    novel_id      : BIGINT FK → Novels.id
    chapter_id    : BIGINT FK → Chapters.id
    line_position : INT NULL          ← MỚI
    paragraph_key : VARCHAR(255) NULL ← MỚI (stable anchor)
    created_at    : DATETIME
    updated_at    : DATETIME NULL     ← MỚI
    UNIQUE KEY (user_id, chapter_id)
}
```

---

## 4. Danh sách file cần sửa / tạo mới

| File | Loại thay đổi | Mô tả |
|---|---|---|
| `domain/Bookmark.java` | **MODIFY** | Thêm field `linePosition`, `paragraphKey`, `updatedAt` |
| `repository/BookmarkRepository.java` | **MODIFY** | Thêm `findByUser_IdAndChapter_Id` |
| `service/LibraryService.java` | **MODIFY** | Thêm `upsertChapterBookmark()`, sửa `getBookmarkItems()` |
| `domain/dto/BookmarkNovelDTO.java` | **MODIFY** | Thêm `linePosition` / `paragraphKey` vào chapter info |
| `controller/client/ClientLibraryController.java` | **MODIFY** | Thêm endpoint POST upsert, giữ toggle cũ hoặc sửa |
| `controller/client/ReaderController.java` | **MODIFY** | Truyền `linePosition` xuống model |
| `templates/client/reader/show.html` | **MODIFY** | JS: đọc scroll, gán `data-key` cho paragraph, scroll khôi phục |
| `templates/client/library/bookmark.html` | **MODIFY** | Link mở bookmark kèm `linePosition` |
| `db/migration/V2__add_line_position.sql` | **NEW** | Migration SQL |

---

## 5. Chi tiết thay đổi từng file

### 5.1 `Bookmark.java` — thêm fields mới

```java
@Column(name = "line_position")
private Integer linePosition;          // index paragraph đã render

@Column(name = "paragraph_key", length = 255)
private String paragraphKey;           // data-key stable anchor

@Column(name = "updated_at")
private LocalDateTime updatedAt;

// Getter & Setter tương ứng...

@PreUpdate
public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

**Backward compatibility:** Cả 3 field đều `NULL` → bookmark cũ không bị ảnh hưởng, fallback về đầu chương khi mở.

---

### 5.2 `BookmarkRepository.java` — thêm query method

```java
// Tìm để UPDATE (upsert)
Optional<Bookmark> findByUser_IdAndChapter_Id(Long userId, Long chapterId);
```

---

### 5.3 `LibraryService.java` — upsertChapterBookmark

```java
@Transactional
public void upsertChapterBookmark(String username, Long novelId, Long chapterId,
                                  Integer linePosition, String paragraphKey) {
    User user = userRepository.findByUsername(username);
    if (user == null) throw new RuntimeException("User not found");

    Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new RuntimeException("Chapter not found"));

    if (chapter.getNovel() == null || !chapter.getNovel().getId().equals(novelId))
        throw new RuntimeException("Chapter does not belong to novel");

    // Upsert: tìm existing → update; không có → insert
    Bookmark bookmark = bookmarkRepository
            .findByUser_IdAndChapter_Id(user.getId(), chapterId)
            .orElseGet(() -> {
                Bookmark b = new Bookmark();
                b.setUser(user);
                b.setNovel(chapter.getNovel());
                b.setChapter(chapter);
                return b;
            });

    bookmark.setLinePosition(linePosition);
    bookmark.setParagraphKey(paragraphKey);
    bookmarkRepository.save(bookmark);
}
```

> **Xóa bookmark:** Giữ nguyên `toggleChapterBookmark` (khi đã có bookmark, click lại → xóa). Hoặc có thể tách endpoint riêng `/bookmark/delete`.

**Sửa `getBookmarkItems()`** để trả thêm `linePosition`:

```java
// Cần DTO mới hoặc mở rộng để mang linePosition theo chapter
// → Xem mục 5.4
```

---

### 5.4 DTO mới: `BookmarkChapterDTO.java`

```java
public class BookmarkChapterDTO {
    private final Chapter chapter;
    private final Integer linePosition;
    private final String  paragraphKey;

    // constructor, getters...
}
```

Sửa `BookmarkNovelDTO`:

```java
public class BookmarkNovelDTO {
    private final Novel novel;
    private final List<BookmarkChapterDTO> chapters; // thay vì List<Chapter>
    // ...
}
```

---

### 5.5 `ClientLibraryController.java` — endpoint upsert

```java
// Endpoint MỚI — nhận linePosition qua request param
@PostMapping("/bookmark/upsert/{novelId}/{chapterId}")
@ResponseBody  // hoặc trả redirect nếu dùng form thường
public ResponseEntity<String> upsertChapterBookmark(
        @PathVariable Long novelId,
        @PathVariable Long chapterId,
        @RequestParam(required = false, defaultValue = "0") Integer linePosition,
        @RequestParam(required = false) String paragraphKey,
        Authentication authentication) {

    libraryService.upsertChapterBookmark(
            authentication.getName(), novelId, chapterId, linePosition, paragraphKey);
    return ResponseEntity.ok("OK");
}

// Endpoint XÓA bookmark (tách khỏi toggle)
@PostMapping("/bookmark/delete/{novelId}/{chapterId}")
public String deleteChapterBookmark(@PathVariable Long novelId,
        @PathVariable Long chapterId, Authentication authentication) {
    libraryService.deleteChapterBookmark(authentication.getName(), novelId, chapterId);
    return "redirect:/reader/" + novelId + "/" + chapterId;
}
```

---

### 5.6 `ReaderController.java` — truyền linePosition xuống view

```java
// Thêm vào getReaderPage():
Integer savedLinePosition = null;
String  savedParagraphKey = null;
if (authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getName())) {
    savedLinePosition = libraryService.getBookmarkLinePosition(
            authentication.getName(), chapterID);
    savedParagraphKey = libraryService.getBookmarkParagraphKey(
            authentication.getName(), chapterID);
}
model.addAttribute("savedLinePosition", savedLinePosition);
model.addAttribute("savedParagraphKey", savedParagraphKey);
```

> Chỉ truyền khi **chính user tự mở chapter từ bookmark**, không phải mọi lần vào chapter.  
> Gợi ý: thêm query param `?from=bookmark` và chỉ scroll khi có param đó.

---

### 5.7 `reader/show.html` — JavaScript

#### A. Gán `data-key` cho từng paragraph khi render

Vì `chapter.content` được render bằng `th:utext` (HTML thô), cần gán key bằng JS sau khi DOM load:

```javascript
document.addEventListener("DOMContentLoaded", function () {
    const content = document.getElementById("chapter-content");
    const paragraphs = content.querySelectorAll("p, div.paragraph");

    paragraphs.forEach((p, index) => {
        if (!p.dataset.key) {
            p.dataset.key = "p-" + index;  // fallback index
        }
    });
});
```

#### B. Khôi phục vị trí đọc khi mở từ bookmark

```javascript
document.addEventListener("DOMContentLoaded", function () {
    // Thymeleaf inject từ model
    const savedKey      = /*[[${savedParagraphKey}]]*/ null;
    const savedPosition = /*[[${savedLinePosition}]]*/ null;
    const fromBookmark  = new URLSearchParams(location.search).get("from") === "bookmark";

    if (!fromBookmark) return;

    const content = document.getElementById("chapter-content");

    function scrollToSaved() {
        // Ưu tiên 1: tìm theo paragraphKey (stable)
        if (savedKey) {
            const target = content.querySelector(`[data-key="${savedKey}"]`);
            if (target) {
                target.scrollIntoView({ behavior: "smooth", block: "start" });
                return;
            }
        }
        // Ưu tiên 2: tìm theo index (linePosition)
        if (savedPosition != null) {
            const paragraphs = content.querySelectorAll("p, div.paragraph");
            const target = paragraphs[savedPosition];
            if (target) {
                target.scrollIntoView({ behavior: "smooth", block: "start" });
                return;
            }
        }
        // Fallback: đầu trang
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    // Chờ fonts + images load xong để tránh layout shift
    if (document.readyState === "complete") {
        scrollToSaved();
    } else {
        window.addEventListener("load", scrollToSaved);
    }
});
```

#### C. Lưu vị trí khi click Bookmark (thay form POST → AJAX)

Sửa form bookmark thành button AJAX:

```html
<!-- Thay thế <form class="bookmark-toggle-form"> hiện tại -->
<button id="bookmark-btn" class="rd_sd-button_item"
        th:attr="data-novel-id=${novel.id}, data-chapter-id=${chapter.id},
                 data-bookmarked=${isBookmarked},
                 data-csrf=${_csrf.token}"
        sec:authorize="isAuthenticated()"
        title="Bookmark chapter">
    <i th:class="${isBookmarked} ? 'fas fa-bookmark' : 'far fa-bookmark'"></i>
</button>
```

```javascript
document.addEventListener("DOMContentLoaded", function () {
    const btn = document.getElementById("bookmark-btn");
    if (!btn) return;

    btn.addEventListener("click", async function () {
        const novelId   = btn.dataset.novelId;
        const chapterId = btn.dataset.chapterId;
        const csrf      = btn.dataset.csrf;
        const isBookmarked = btn.dataset.bookmarked === "true";

        if (isBookmarked) {
            // Xóa bookmark
            await fetch(`/bookshelf/bookmark/delete/${novelId}/${chapterId}`, {
                method: "POST",
                headers: { "X-CSRF-TOKEN": csrf }
            });
            btn.dataset.bookmarked = "false";
            btn.querySelector("i").className = "far fa-bookmark";
            return;
        }

        // Lấy vị trí đọc hiện tại
        const content    = document.getElementById("chapter-content");
        const paragraphs = [...content.querySelectorAll("p, div.paragraph")];
        const scrollY    = window.scrollY;

        let linePosition  = 0;
        let paragraphKey  = null;

        // Tìm paragraph gần nhất với viewport
        for (let i = 0; i < paragraphs.length; i++) {
            const rect = paragraphs[i].getBoundingClientRect();
            if (rect.top >= 0) {
                linePosition = i;
                paragraphKey = paragraphs[i].dataset.key || ("p-" + i);
                break;
            }
        }

        const params = new URLSearchParams({
            linePosition, paragraphKey,
            _csrf: csrf
        });

        const res = await fetch(
            `/bookshelf/bookmark/upsert/${novelId}/${chapterId}`,
            { method: "POST", body: params }
        );

        if (res.ok) {
            btn.dataset.bookmarked = "true";
            btn.querySelector("i").className = "fas fa-bookmark";
        }
    });
});
```

---

### 5.8 `library/bookmark.html` — link mở với scroll

```html
<!-- Hiện tại -->
<a th:href="@{/reader/{novelId}/{chapterId}(novelId=...,chapterId=...)}">

<!-- Thay bằng — thêm ?from=bookmark để trigger scroll -->
<a th:href="@{/reader/{novelId}/{chapterId}(novelId=${item.novel.id},
             chapterId=${chapter.chapterId},
             from='bookmark')}"
   th:text="${chapter.chapterTitle}">
</a>
```

---

### 5.9 UI Bookmark Indicator — Ký hiệu bookmark cạnh dòng đọc

#### Mục tiêu

Khi user đang đọc chapter và đã có bookmark, hiển thị **một ký hiệu nhỏ (icon bookmark)** ngay cạnh trái của đoạn văn (paragraph) đã được đánh dấu, giúp user nhìn thấy ngay vị trí họ đã lưu mà không cần phải scroll tìm.

#### Thiết kế UI

```
│  ← (lề trái chapter)
│
│  🔖  Đây là đoạn văn bản được đánh dấu bookmark...
│      Lorem ipsum dolor sit amet, consectetur adipiscing
│      elit, sed do eiusmod tempor incididunt...
│
│      Đoạn tiếp theo bình thường, không có icon...
```

- Icon nằm **absolute bên trái** của paragraph, không đẩy lệch text
- Dùng **FontAwesome** `fa-bookmark` (đã có sẵn trong dự án)
- Màu nhấn (accent), nhỏ gọn, không che khuất nội dung
- Có **tooltip** khi hover: `"Vị trí bookmark của bạn"`
- Khi bookmark bị xóa → icon **tự biến mất** (không cần reload)

---

#### A. Cấu trúc HTML — thêm `position: relative` cho paragraph

Paragraph được gán `data-key` (từ mục 5.7A) cần thêm class `bm-paragraph` để CSS target:

```javascript
// Trong đoạn gán data-key (mục 5.7A), bổ sung:
paragraphs.forEach((p, index) => {
    if (!p.dataset.key) {
        p.dataset.key = "p-" + index;
    }
    p.classList.add("bm-paragraph");  // ← thêm class để CSS định vị
});
```

---

#### B. CSS — `chapter.css` (thêm vào cuối file)

File: `src/main/resources/static/client/css/chapter.css`

```css
/* ── Bookmark Indicator ─────────────────────────────── */
.bm-paragraph {
    position: relative; /* bắt buộc để icon absolute hoạt động */
}

.bm-line-indicator {
    position: absolute;
    left: -28px;          /* đẩy ra ngoài lề trái text */
    top: 2px;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #e8845a;       /* màu accent bookmark — điều chỉnh theo theme */
    font-size: 13px;
    opacity: 0;
    transform: translateX(-4px);
    transition: opacity 0.25s ease, transform 0.25s ease;
    pointer-events: auto;
    cursor: default;
    z-index: 10;
}

.bm-line-indicator.visible {
    opacity: 1;
    transform: translateX(0);
}

.bm-line-indicator:hover::after {
    content: "Vị trí bookmark của bạn";
    position: absolute;
    left: 24px;
    top: 50%;
    transform: translateY(-50%);
    background: rgba(0, 0, 0, 0.75);
    color: #fff;
    font-size: 11px;
    white-space: nowrap;
    padding: 3px 8px;
    border-radius: 4px;
    pointer-events: none;
}

/* Đảm bảo #chapter-content có đủ padding-left để icon không bị crop */
#chapter-content {
    padding-left: 36px !important;
}
```

> [!NOTE]
> Nếu theme hiện tại đã có `padding-left` cho `#chapter-content`, kiểm tra lại giá trị `left` của `.bm-line-indicator` để không bị ẩn ngoài viewport.

---

#### C. JavaScript — render và cập nhật indicator

Thêm vào `reader/show.html`, trong block `DOMContentLoaded`:

```javascript
// ── Bookmark Line Indicator ──────────────────────────────
const INDICATOR_ID = "bm-indicator-icon";

function renderBookmarkIndicator(paragraphKey, linePosition) {
    // Xóa indicator cũ nếu có
    const old = document.getElementById(INDICATOR_ID);
    if (old) old.remove();

    if (paragraphKey == null && linePosition == null) return;

    const content    = document.getElementById("chapter-content");
    const paragraphs = content.querySelectorAll(".bm-paragraph");

    // Tìm đúng paragraph
    let target = null;
    if (paragraphKey) {
        target = content.querySelector(`[data-key="${paragraphKey}"]`);
    }
    if (!target && linePosition != null) {
        target = paragraphs[linePosition] || null;
    }
    if (!target) return;

    // Tạo icon
    const icon = document.createElement("span");
    icon.id        = INDICATOR_ID;
    icon.className = "bm-line-indicator";
    icon.innerHTML = '<i class="fas fa-bookmark"></i>';
    icon.setAttribute("title", "Vị trí bookmark của bạn");

    target.appendChild(icon);

    // Animate vào
    requestAnimationFrame(() => {
        requestAnimationFrame(() => icon.classList.add("visible"));
    });
}

function removeBookmarkIndicator() {
    const icon = document.getElementById(INDICATOR_ID);
    if (!icon) return;
    icon.classList.remove("visible");
    setTimeout(() => icon.remove(), 300); // chờ transition
}
```

---

#### D. Tích hợp với luồng Bookmark AJAX (mục 5.7C)

Sửa lại callback sau khi upsert/xóa thành công:

```javascript
// Sau khi upsert thành công:
if (res.ok) {
    btn.dataset.bookmarked = "true";
    btn.querySelector("i").className = "fas fa-bookmark";

    // ← Hiển thị indicator tại vị trí vừa bookmark
    renderBookmarkIndicator(paragraphKey, linePosition);
}

// Sau khi xóa bookmark thành công:
// (thêm vào sau fetch delete)
btn.dataset.bookmarked = "false";
btn.querySelector("i").className = "far fa-bookmark";

// ← Ẩn indicator
removeBookmarkIndicator();
```

---

#### E. Tích hợp khi load chapter có bookmark sẵn

Khi user mở chapter (dù từ bookmark hay gõ URL trực tiếp), nếu đã có `savedParagraphKey` thì render indicator ngay:

```javascript
// Thêm vào cuối block DOMContentLoaded — sau khi gán data-key xong:
const savedKey      = /*[[${savedParagraphKey}]]*/ null;
const savedPosition = /*[[${savedLinePosition}]]*/ null;
const isBookmarked  = /*[[${isBookmarked}]]*/ false;

if (isBookmarked) {
    // Render ngay, không cần fromBookmark
    renderBookmarkIndicator(savedKey, savedPosition);
}
```

> [!IMPORTANT]
> `isBookmarked` đã được `ReaderController` inject vào model — không cần request thêm.

---

#### F. Khi user cập nhật bookmark lên dòng mới

Nếu user đang đọc và click bookmark lần 2 (upsert → di chuyển vị trí), indicator phải **nhảy** sang paragraph mới:

```javascript
// renderBookmarkIndicator() đã xử lý:
// → Xóa old indicator trước → append vào target mới
// → Không để lại 2 icon cùng lúc
```

Không cần xử lý thêm — hàm `renderBookmarkIndicator` tự dọn icon cũ.

---

#### G. Mobile — điều chỉnh vị trí

Trên màn hình nhỏ, `left: -28px` có thể bị ẩn ngoài viewport. Thêm media query:

```css
@media (max-width: 576px) {
    .bm-line-indicator {
        left: 2px;      /* đặt vào trong, không ra ngoài lề */
        top: -18px;     /* hiển thị phía trên paragraph */
        font-size: 11px;
    }

    #chapter-content {
        padding-left: 16px !important; /* giảm padding trên mobile */
    }
}
```

---

## 6. Flow hoàn chỉnh

### 6.1 Lưu bookmark

```
User đang đọc chương 15, kéo tới dòng 350
│
├─ Click nút Bookmark (JS intercept)
│   ├─ Tính linePosition = 350, paragraphKey = "p-350"
│   └─ POST /bookshelf/bookmark/upsert/1/15
│        { linePosition=350, paragraphKey="p-350" }
│
├─ ClientLibraryController.upsertChapterBookmark()
│   └─ LibraryService.upsertChapterBookmark()
│       ├─ findByUser_IdAndChapter_Id(userId, 15)
│       │   ├─ Tìm thấy → UPDATE linePosition=350, updatedAt=now
│       │   └─ Không có → INSERT mới
│       └─ bookmarkRepository.save(bookmark)
│
└─ Response: 200 OK → UI cập nhật icon bookmark
```

### 6.2 Mở bookmark

```
User vào /bookshelf/bookmark
│
├─ Danh sách hiện: [Chapter 15 - linePosition=350]
│
└─ Click link → /reader/1/15?from=bookmark
    │
    ├─ ReaderController.getReaderPage()
    │   ├─ Load chapter content
    │   ├─ savedLinePosition = libraryService.getBookmarkLinePosition(user, 15) → 350
    │   ├─ savedParagraphKey = "p-350"
    │   └─ model: savedLinePosition=350, savedParagraphKey="p-350"
    │
    └─ show.html render → JS chạy sau DOMContentLoaded + window.load
        ├─ from=bookmark → kích hoạt scroll
        ├─ Tìm [data-key="p-350"] → scrollIntoView
        └─ Fallback: scrollTo(0,0) nếu key không tồn tại
```

---

## 7. Edge Cases & Xử lý

| Tình huống | Xử lý |
|---|---|
| Chapter cập nhật nội dung → paragraph lệch index | `paragraphKey` (data-key) ưu tiên hơn index. Nếu cả hai fail → scroll về đầu |
| Race condition: user click bookmark 2 lần liên tiếp | UNIQUE constraint DB + upsert logic đảm bảo chỉ 1 record. Front-end: disable button trong lúc request pending |
| User chưa đăng nhập | Nút bookmark không render (Thymeleaf `sec:authorize`) |
| Bookmark cũ (trước khi có linePosition) | `linePosition = null`, `paragraphKey = null` → fallback đầu chapter, không crash |
| Mobile: scroll API khác | `scrollIntoView({ behavior: "smooth" })` hoạt động trên cả mobile/web |
| Chapter rất dài, fonts chưa load → layout shift | `window.addEventListener("load", scrollToSaved)` chờ toàn bộ tài nguyên |
| User mở chapter bình thường (không từ bookmark) | Query param `from=bookmark` không có → JS không scroll, UX không thay đổi |
| Xóa bookmark | Endpoint `/bookmark/delete/{novelId}/{chapterId}` riêng, không ảnh hưởng toggle cũ |
| **Indicator bị crop khỏi viewport (mobile)** | Media query `@media (max-width: 576px)` đổi icon lên phía trên paragraph |
| **User bookmark lại → indicator nhảy sang dòng mới** | `renderBookmarkIndicator()` tự xóa icon cũ trước khi append vào target mới |
| **Chapter không có `<p>` tag (chỉ có text node)** | Selector fallback: `querySelectorAll("p, div, br")` — kiểm tra cấu trúc `chapter.content` thực tế |
| **`#chapter-content` padding-left ban đầu < 36px** | CSS `!important` override, hoặc điều chỉnh `left` của `.bm-line-indicator` cho phù hợp |
| **Nhiều tab mở cùng chapter** | Indicator là client-only, mỗi tab render độc lập từ `savedParagraphKey` inject bởi server |

---

## 8. Tóm tắt thay đổi

### Files sửa (MODIFY)

| File | Thay đổi chính |
|---|---|
| `Bookmark.java` | + `linePosition`, `paragraphKey`, `updatedAt` |
| `BookmarkRepository.java` | + `findByUser_IdAndChapter_Id()` |
| `LibraryService.java` | + `upsertChapterBookmark()`, `getBookmarkLinePosition()`, `getBookmarkParagraphKey()` |
| `BookmarkNovelDTO.java` | Dùng `BookmarkChapterDTO` thay `Chapter` |
| `ClientLibraryController.java` | + endpoint `/bookmark/upsert/`, `/bookmark/delete/` |
| `ReaderController.java` | + truyền `savedLinePosition`, `savedParagraphKey` vào model |
| `reader/show.html` | Đổi form → AJAX button; + JS gán `data-key`, scroll restore, render/remove indicator |
| `library/bookmark.html` | Link thêm `?from=bookmark`; hiển thị `linePosition` |
| `static/client/css/chapter.css` | + `.bm-paragraph`, `.bm-line-indicator`, media query mobile |

### Files tạo mới (NEW)

| File | Nội dung |
|---|---|
| `domain/dto/BookmarkChapterDTO.java` | DTO mang chapter + linePosition + paragraphKey |
| `db/migration/V2__add_line_position.sql` | ALTER TABLE Bookmark thêm 3 cột mới |

### Không thay đổi

- Schema bảng `Chapters` (không cần sửa)  
- Logic bookshelf (tủ truyện) hoàn toàn độc lập  
- UI trang bookmark (chỉ thêm param vào link, layout không đổi)  
- Constraint `UNIQUE(user_id, chapter_id)` giữ nguyên

---

> [!IMPORTANT]
> **Backward Compatibility:** Tất cả bookmark cũ vẫn hoạt động bình thường. Khi `linePosition = null`, hệ thống fallback về đầu chương — không crash, không mất dữ liệu.

> [!TIP]
> **Thứ tự thực hiện khuyên dùng:**  
> 1. Chạy migration SQL  
> 2. Sửa `Bookmark.java` + `BookmarkRepository.java`  
> 3. Thêm `BookmarkChapterDTO.java`  
> 4. Sửa `LibraryService.java`  
> 5. Sửa `ClientLibraryController.java` + `ReaderController.java`  
> 6. Sửa template `show.html` (JS) + `bookmark.html` (link)  
> 7. Test: bookmark → đóng tab → mở lại từ bookmark page → kiểm tra scroll
