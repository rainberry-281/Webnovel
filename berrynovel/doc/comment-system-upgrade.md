# Comment System Upgrade — Documentation

## Overview

Nâng cấp hệ thống comment hiện có để hỗ trợ comment ở cả hai nơi:

1. **Trang novel detail** (`/novel/{id}`) — comment gắn với tiểu thuyết
2. **Trang chapter reader** (`/reader/{novelId}/{chapterId}`) — comment gắn với chương cụ thể

Trang novel detail hiển thị **tất cả comment** của cả tiểu thuyết lẫn các chương, gộp chung theo thứ tự mới nhất.
Mỗi comment từ chapter hiển thị thêm một dòng nhỏ màu xám kèm link đến chương đó.

---

## Trạng thái hiện tại (Before)

### `Comment.java` — Entity hiện tại

| Field | Type | Column |
|---|---|---|
| `id` | `int` | `id` (PK, auto) |
| `content` | `String` | `TEXT` |
| `user` | `User` | FK `user_id` |
| `novel` | `Novel` | FK `novel_id` |
| `createdAt` | `LocalDateTime` | `created_at` |

**Vấn đề:** Không có liên kết đến `Chapter` → không thể phân biệt novel-level comment với chapter-level comment.

### `CommentRepository.java` — Hiện tại

```java
List<Comment> findByNovel_IdOrderByCreatedAtDesc(Long novelId);
```

Chỉ query theo `novelId`, không có filter theo chapter.

### `CommentService.java` — Hiện tại

| Method | Mô tả |
|---|---|
| `getCommentsByNovelId(Long novelId)` | Lấy tất cả comment của novel (hiện tại không phân biệt chapter) |
| `createComment(Long novelId, String username, String content)` | Tạo comment novel-level |
| `deleteComment(Long novelId, Integer commentId, String username, boolean isAdmin)` | Xóa comment |

### `ClientNovelController.java` — Route hiện tại

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/novel/{id}` | Hiển thị trang detail, load comments qua `commentService.getCommentsByNovelId(id)` |
| `POST` | `/novel/{id}/comment` | Tạo novel-level comment |
| `POST` | `/novel/{novelId}/comment/{commentId}/delete` | Xóa comment |

### `ReaderController.java` — Hiện tại

Không có logic comment nào. Không load comment cho chapter, không có endpoint POST comment chapter.

---

## Thay đổi cần thực hiện (After)

### 1. Comment Entity — Thêm field `chapter`

```java
// Thêm vào Comment.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "chapter_id")
private Chapter chapter;  // nullable — null = novel-level, non-null = chapter-level
```

Getter/setter tương ứng.

**Quy tắc:**
- `comment.chapter == null` → novel-level comment (đăng từ trang novel detail)
- `comment.chapter != null` → chapter-level comment (đăng từ trang reader)
- Mọi comment đều bắt buộc có `novel` (không nullable)

### 2. Database Migration

Cột mới cần thêm vào bảng `Comments`:

```sql
ALTER TABLE Comments
    ADD COLUMN chapter_id BIGINT DEFAULT NULL,
    ADD CONSTRAINT fk_comments_chapter
        FOREIGN KEY (chapter_id) REFERENCES Chapters(id)
        ON DELETE SET NULL;
```

> Dùng `ON DELETE SET NULL` để nếu chapter bị xóa, comment vẫn tồn tại nhưng không còn liên kết chapter.

### 3. CommentRepository — Thêm query methods

```java
// Query tất cả comment của novel (dùng cho trang detail — bao gồm cả chapter comment)
List<Comment> findByNovel_IdOrderByCreatedAtDesc(Long novelId);
// Method này đã tồn tại, không cần thêm — chỉ cần Hibernate tự load chapter field

// Query comment của 1 chapter cụ thể (dùng cho trang reader)
List<Comment> findByChapter_IdOrderByCreatedAtDesc(Long chapterId);

// Tùy chọn: query chỉ novel-level comments (không có chapter)
List<Comment> findByNovel_IdAndChapterIsNullOrderByCreatedAtDesc(Long novelId);
```

### 4. CommentService — Thêm methods

```java
// Tạo chapter-level comment
public Comment createChapterComment(Long novelId, Long chapterId, String username, String content)

// Lấy comment của chapter (dùng cho reader page)
public List<Comment> getCommentsByChapterId(Long chapterId)
```

**Validation trong `createChapterComment`:**
- `content` không được blank
- `content` trim, max 2000 ký tự
- `user` phải tồn tại
- `novel` phải tồn tại
- `chapter` phải tồn tại và phải thuộc về `novel` đó (kiểm tra `chapter.getNovel().getId().equals(novelId)`)

### 5. ClientNovelController — Không đổi logic hiện tại

Method `getCommentsByNovelId` đã load tất cả comment theo novelId (kể cả chapter comment sau khi entity có field `chapter`).
Chỉ cần đảm bảo Hibernate load `chapter` field khi render Thymeleaf (dùng `FetchType.LAZY` + `th:if` sẽ trigger load).

### 6. ReaderController — Thêm comment logic

**GET `/reader/{novelId}/{chapterId}`** — Thêm vào model:

```java
model.addAttribute("chapterComments", commentService.getCommentsByChapterId(chapterId));
```

**POST `/reader/{novelId}/{chapterId}/comments`** — Endpoint mới:

```java
@PostMapping("/{novelID}/{chapterID}/comments")
public String createChapterComment(
        @PathVariable Long novelID,
        @PathVariable Long chapterID,
        @RequestParam("content") String content,
        Authentication authentication) {
    // guard: redirect to login nếu chưa đăng nhập
    // gọi commentService.createChapterComment(...)
    // redirect: /reader/{novelID}/{chapterID}#comments
}
```

---

## Template Changes

### `client/novel/show.html` — Hiển thị chapter reference

Trong vòng lặp comment list, thêm block hiển thị chapter reference khi `comment.chapter != null`:

```html
<div class="comment-item" th:each="comment : ${comments}">
    <!-- ... avatar, username, time, delete button ... -->

    <!-- Chapter reference — chỉ hiện nếu comment đến từ chapter -->
    <div class="comment-chapter-ref" th:if="${comment.chapter != null}">
        <a th:href="@{/reader/{novelId}/{chapterId}(novelId=${novel.id}, chapterId=${comment.chapter.id})}"
           th:text="${comment.chapter.title}">
        </a>
    </div>

    <div class="comment-text" th:text="${comment.content}"></div>
</div>
```

### `client/reader/show.html` — Thêm comment section

Thêm section comment vào cuối phần nội dung chương (sau `#chapter-content`, trước `</div>` của `.reading-content`):

```html
<section id="comments" class="chapter-comments mt-4">
    <h3 class="chapter-comments-title">Comments</h3>

    <!-- Form — chỉ hiện nếu đã đăng nhập -->
    <form sec:authorize="isAuthenticated()"
          class="chapter-comment-form"
          th:action="@{/reader/{novelId}/{chapterId}/comments(novelId=${novel.id}, chapterId=${chapter.id})}"
          method="post">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <textarea name="content" class="chapter-comment-textarea"
                  rows="3" maxlength="2000" required
                  placeholder="Write a comment..."></textarea>
        <div class="chapter-comment-actions">
            <button type="submit" class="chapter-comment-submit">Post Comment</button>
        </div>
    </form>

    <!-- Login prompt -->
    <div sec:authorize="!isAuthenticated()" class="chapter-comment-login">
        <a th:href="@{/login}">Log in</a> to comment.
    </div>

    <!-- Comment list -->
    <div class="chapter-comment-list">
        <div th:if="${#lists.isEmpty(chapterComments)}" class="chapter-comment-empty">
            No comments yet.
        </div>
        <div class="chapter-comment-item" th:each="comment : ${chapterComments}">
            <div class="chapter-comment-meta">
                <span class="chapter-comment-author"
                      th:text="${comment.user != null ? comment.user.username : 'Unknown'}"></span>
                <span class="chapter-comment-time"
                      th:text="${comment.createdAt != null ? #temporals.format(comment.createdAt, 'dd/MM/yyyy HH:mm') : ''}"></span>
            </div>
            <div class="chapter-comment-content" th:text="${comment.content}"></div>
        </div>
    </div>
</section>
```

> **Bảo mật:** Luôn dùng `th:text` (không dùng `th:utext`) để hiển thị nội dung comment. Điều này ngăn XSS injection.

---

## CSS Changes

### `novel_detail.css` — Thêm `.comment-chapter-ref`

```css
/* Chapter reference badge trong comment list của trang novel detail */
.comment-chapter-ref {
    font-size: 0.82rem;
    color: #999;
    margin-top: 4px;
    margin-left: 46px;
    margin-bottom: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.comment-chapter-ref a {
    color: #999;
    text-decoration: none;
}

.comment-chapter-ref a:hover {
    color: #666;
    text-decoration: underline;
}
```

### `chapter.css` — Thêm comment styles

Thêm block CSS cho chapter comment section:

```css
/* =============================================
   Chapter Page — Comment Section
   ============================================= */

.chapter-comments {
    padding: 20px 0;
    border-top: 1px solid #e0d4c4;
}

.chapter-comments-title {
    font-size: 1.1rem;
    font-weight: 700;
    color: #444;
    margin-bottom: 16px;
}

.chapter-comment-form {
    margin-bottom: 20px;
}

.chapter-comment-textarea {
    width: 100%;
    border: 1px solid #d0c8bc;
    border-radius: 6px;
    padding: 8px 10px;
    font-size: 15px;
    background: #fffdf9;
    resize: vertical;
    font-family: inherit;
}

.chapter-comment-textarea:focus {
    outline: none;
    border-color: #a0988c;
}

.chapter-comment-actions {
    text-align: right;
    margin-top: 8px;
}

.chapter-comment-submit {
    background: #5a5047;
    color: #fff;
    border: none;
    border-radius: 5px;
    padding: 7px 18px;
    font-size: 14px;
    cursor: pointer;
    transition: background 0.2s ease;
}

.chapter-comment-submit:hover {
    background: #3d342d;
}

.chapter-comment-login {
    color: #888;
    font-size: 14px;
    margin-bottom: 16px;
}

.chapter-comment-login a {
    color: #5a5047;
    text-decoration: none;
}

.chapter-comment-login a:hover {
    text-decoration: underline;
}

.chapter-comment-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.chapter-comment-empty {
    color: #aaa;
    font-size: 14px;
}

.chapter-comment-item {
    padding: 10px 0;
    border-bottom: 1px solid #e8ddd0;
}

.chapter-comment-item:last-child {
    border-bottom: none;
}

.chapter-comment-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 4px;
}

.chapter-comment-author {
    font-weight: 600;
    font-size: 14px;
    color: #444;
}

.chapter-comment-time {
    font-size: 12px;
    color: #aaa;
}

.chapter-comment-content {
    font-size: 15px;
    color: #555;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
}
```

---

## Endpoints Summary

| Method | URL | Controller | Mô tả |
|---|---|---|---|
| `GET` | `/novel/{id}` | `ClientNovelController` | Load trang detail, **không đổi** — đã load `comments` |
| `POST` | `/novel/{id}/comment` | `ClientNovelController` | Tạo novel-level comment — **không đổi** |
| `POST` | `/novel/{novelId}/comment/{commentId}/delete` | `ClientNovelController` | Xóa comment — **không đổi** |
| `GET` | `/reader/{novelId}/{chapterId}` | `ReaderController` | **Thêm** `chapterComments` vào model |
| `POST` | `/reader/{novelId}/{chapterId}/comments` | `ReaderController` | **Mới** — tạo chapter-level comment |

---

## Luồng hoạt động

### Đăng comment từ trang reader (chapter)

```
User điền form → POST /reader/{novelId}/{chapterId}/comments
→ ReaderController.createChapterComment()
    → commentService.createChapterComment(novelId, chapterId, username, content)
        → validate content, user, novel, chapter ownership
        → comment.setNovel(novel), comment.setChapter(chapter)
        → save
→ redirect: /reader/{novelId}/{chapterId}#comments
```

### Hiển thị comment trên trang novel detail

```
GET /novel/{id}
→ ClientNovelController.getNovelDetailPage()
    → commentService.getCommentsByNovelId(id)
        → findByNovel_IdOrderByCreatedAtDesc(id)
        → trả về List<Comment> gồm CẢ novel-level và chapter-level
→ Thymeleaf render:
    th:each comment:
        nếu comment.chapter != null → hiện .comment-chapter-ref + link đến reader
        luôn hiện comment.content (th:text, không phải th:utext)
```

---

## Security

| Vấn đề | Giải pháp |
|---|---|
| XSS injection qua comment content | `th:text` trên tất cả comment content fields |
| CSRF | `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">` trong mọi form |
| Anonymous comment | Controller check `authentication == null or anonymousUser` → redirect `/login` |
| Chapter không thuộc novel | `createChapterComment` verify `chapter.getNovel().getId().equals(novelId)` |
| Xóa comment của người khác | `deleteComment` check `comment.user.username == authentication.name` trước khi cho phép |

---

## Files Changed

| File | Loại thay đổi |
|---|---|
| `domain/Comment.java` | Thêm field `chapter`, getter/setter |
| `repository/CommentRepository.java` | Thêm `findByChapter_IdOrderByCreatedAtDesc` |
| `service/CommentService.java` | Thêm `createChapterComment`, `getCommentsByChapterId` |
| `controller/client/ReaderController.java` | Thêm `chapterComments` vào model GET, thêm POST endpoint |
| `templates/client/novel/show.html` | Thêm `.comment-chapter-ref` block trong comment list |
| `templates/client/reader/show.html` | Thêm `#comments` section |
| `static/client/css/novel_detail.css` | Thêm `.comment-chapter-ref` CSS |
| `static/client/css/chapter.css` | Thêm chapter comment section CSS |
| Database `Comments` table | Thêm cột `chapter_id` (migration SQL) |

**Không thay đổi:**
- Bookmark logic
- TTS reader
- localStorage reading history
- Read count logic
- Hot tag logic
- Admin controllers
- Novel listing / category / search

---

## Edge Cases

| Tình huống | Xử lý |
|---|---|
| Chapter bị xóa sau khi comment | `ON DELETE SET NULL` → `comment.chapter = null`, không hiện chapter ref |
| User bị xóa | `comment.user = null` → template check `comment.user != null ? ... : 'Unknown'` |
| Comment blank | Service validate `content.trim().isEmpty()` → return null hoặc throw |
| Comment quá dài | Validate max 2000 ký tự trong service, `maxlength="2000"` trên textarea |
| Chapter không thuộc novel | Service kiểm tra ownership trước khi save |
| Người dùng chưa đăng nhập POST comment | Controller redirect `/login` |
| Danh sách comment trống | Template check `#lists.isEmpty(...)` → hiện "No comments yet." |
