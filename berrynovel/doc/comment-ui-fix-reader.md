# Comment UI Fix — Reader Page

## Mô tả vấn đề

Comment section trên trang reader (`/reader/{novelId}/{chapterId}`) hiển thị sai layout:

- Avatar nổi ở giữa (do `align-items: center` trên `.chapter-comment-user`)
- Username và thời gian bị đẩy quá xa
- Nội dung comment (`.chapter-comment-content`) tách rời khỏi phần user info — chỉ dùng `margin-left: 46px` để giả thụt vào, thay vì nằm trong cùng một flex-body container
- Không có wrapper "body" để gom username + time + content lại thành một khối

Trang **novel detail** (`/novel/{id}`) dùng cấu trúc `comment-user → comment-meta → comment-header` hoạt động đúng — nên reader page cần được căn chỉnh theo pattern đó.

---

## Nguyên nhân gốc rễ

### Cấu trúc HTML hiện tại (`reader/show.html`, lines 177–190)

```
.chapter-comment-item                     ← không có display:flex
  ├── .chapter-comment-user               ← display:flex; align-items:CENTER ← vấn đề
  │     ├── img.chapter-comment-avatar
  │     └── .chapter-comment-meta         ← flex; chỉ chứa author + time
  └── .chapter-comment-content            ← NGOÀI .chapter-comment-user, margin-left:46px
```

**Vấn đề cụ thể:**

1. `.chapter-comment-item` thiếu `display: flex` — avatar và content không xếp hàng ngang
2. `.chapter-comment-user` dùng `align-items: center` → avatar bị căn giữa dọc so với meta block
3. Nội dung comment (`.chapter-comment-content`) là **sibling** của `.chapter-comment-user`, không phải con → nó không nằm trong cùng flow với avatar, chỉ dùng `margin-left: 46px` để giả thụt vào. Khi layout thay đổi hoặc avatar thay đổi kích thước, canh lề vỡ

### So sánh với novel/show.html (hoạt động đúng)

```
.comment-item                             ← không flex (block bình thường)
  └── .comment-user                       ← display:flex; align-items:center; gap:10px
        ├── img.comment-avatar            ← 36×36px, flex-shrink:0
        └── .comment-meta                 ← flex:1
              ├── .comment-header         ← flex; justify-content:space-between
              │     ├── .comment-name
              │     └── .comment-actions  ← time + delete btn
              └── (content ở ngoài, dùng margin-left:46px giống reader)
```

Novel/show cũng có `margin-left: 46px` cho `.comment-text` — nhưng layout vẫn ổn vì `.comment-user` đã làm đúng việc xếp avatar + meta ngang hàng với `align-items: center`.

**Vấn đề thực sự của reader page là:**
- `.chapter-comment-meta` (chứa author + time) nằm bên trong `.chapter-comment-user` flex container với `align-items: center` → meta block bị căn giữa theo chiều dọc so với avatar → có cảm giác avatar nổi ở giữa
- `.chapter-comment-meta` tự nó cũng là flex với `align-items: center` → author và time nằm ngang hàng nhau (đúng)
- Nhưng không có `justify-content: space-between` → author và time chen vào nhau ở bên trái

**Pattern đúng** cần áp dụng cho reader: `comment-item` → `flex row` → `[avatar | body-column]` → trong body: `[header row: author + time]` + `[content block]`

---

## Files Thay Đổi

| File | Loại | Thay đổi |
|---|---|---|
| `templates/client/reader/show.html` | Template | Tái cấu trúc HTML của comment item |
| `static/client/css/chapter.css` | CSS | Thay thế các class `chapter-comment-*` bị sai |

**Không thay đổi:**
- Backend controller
- POST endpoint comment
- CSRF
- Bookmark logic
- TTS reader
- Reading history
- Read count

---

## Cấu trúc HTML Sau Khi Fix

```
.chapter-comment-item                     ← display:flex; align-items:flex-start; gap:12px
  ├── .chapter-comment-avatar-wrapper     ← flex:0 0 38px; chứa <img>
  │     └── img                           ← 38×38px, border-radius:50%
  └── .chapter-comment-body               ← flex:1; min-width:0 — gom TOÀN BỘ nội dung
        ├── .chapter-comment-header       ← flex; align-items:baseline; gap:8px
        │     ├── .chapter-comment-author ← font-weight:600
        │     └── .chapter-comment-time   ← font-size:12px; color:#aaa
        └── .chapter-comment-content      ← margin-top:5px; nằm BÊN TRONG body
```

### HTML mới (thay thế lines 177–190 trong reader/show.html)

```html
<div class="chapter-comment-item" th:each="comment : ${chapterComments}">
    <div class="chapter-comment-avatar-wrapper">
        <img class="chapter-comment-avatar"
             th:src="@{/images/avatar/{image}(image=${comment.user != null and comment.user.image != null and !#strings.isEmpty(comment.user.image) ? comment.user.image : 'defaultavatar.png'},v=${#dates.createNow().time})}"
             alt="Avatar">
    </div>

    <div class="chapter-comment-body">
        <div class="chapter-comment-header">
            <span class="chapter-comment-author"
                  th:text="${comment.user != null ? (comment.user.username != null and !#strings.isEmpty(comment.user.username) ? comment.user.username : 'Unknown') : 'Unknown'}"></span>
            <span class="chapter-comment-time"
                  th:text="${comment.createdAt != null ? #temporals.format(comment.createdAt, 'dd/MM/yyyy HH:mm') : ''}"></span>
        </div>

        <div class="chapter-comment-content" th:text="${comment.content}"></div>
    </div>
</div>
```

**Điểm khác biệt quan trọng:**
- Thêm wrapper `chapter-comment-avatar-wrapper` (cố định width)
- Thêm `chapter-comment-body` gom toàn bộ header + content
- `chapter-comment-content` nằm **bên trong** `chapter-comment-body` → không cần `margin-left` giả nữa
- Dùng `th:text` cho content (không dùng `th:utext`) — bảo mật XSS

---

## CSS Sau Khi Fix

### Xóa/thay thế các class lỗi trong `chapter.css`

**Các class cần thay đổi** (hiện tại từ line 596–646):

```css
/* TRƯỚC — cấu trúc lỗi */
.chapter-comment-item { padding: 12px 0; border-bottom: 1px solid #e8ddd0; }
.chapter-comment-user { display: flex; align-items: center; gap: 10px; }
.chapter-comment-avatar { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; object-fit: cover; }
.chapter-comment-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; min-width: 0; }
.chapter-comment-author { color: #444; font-size: 14px; font-weight: 600; }
.chapter-comment-time { color: #aaa; font-size: 12px; }
.chapter-comment-content {
    color: #555; font-size: 15px; line-height: 1.5;
    margin-left: 46px;   /* ← giả thụt, không cần nữa */
    margin-top: 5px;
    white-space: pre-wrap; word-break: break-word;
}
```

**Thay bằng:**

```css
/* SAU — cấu trúc đúng */

.chapter-comment-item {
    display: flex;
    align-items: flex-start;       /* avatar ghim trên cùng */
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #e8ddd0;
}

.chapter-comment-item:last-child {
    border-bottom: none;
}

.chapter-comment-avatar-wrapper {
    flex: 0 0 38px;
    width: 38px;
    height: 38px;
}

.chapter-comment-avatar {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    object-fit: cover;
    display: block;
}

.chapter-comment-body {
    flex: 1;
    min-width: 0;                  /* ngăn overflow trong flex */
}

.chapter-comment-header {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 5px;
    flex-wrap: wrap;
}

.chapter-comment-author {
    font-weight: 600;
    font-size: 14px;
    color: #333;
}

.chapter-comment-time {
    font-size: 12px;
    color: #aaa;
    white-space: nowrap;
}

.chapter-comment-content {
    font-size: 15px;
    line-height: 1.6;
    color: #555;
    white-space: pre-wrap;
    word-break: break-word;
    /* KHÔNG cần margin-left — content đã nằm trong .chapter-comment-body */
}
```

**Mobile:**

```css
@media (max-width: 576px) {
    .chapter-comment-item {
        gap: 10px;
    }

    .chapter-comment-avatar-wrapper,
    .chapter-comment-avatar {
        width: 32px;
        height: 32px;
        flex-basis: 32px;
    }

    .chapter-comment-header {
        gap: 4px 6px;
    }
}
```

---

## So sánh Before / After

### HTML Structure

| | Before (lỗi) | After (fix) |
|---|---|---|
| Item container | block, không flex | `display:flex; align-items:flex-start; gap:12px` |
| Avatar holder | `.chapter-comment-user` (flex, align-items:center) | `.chapter-comment-avatar-wrapper` (fixed 38px) |
| Body holder | không có | `.chapter-comment-body` (flex:1) |
| Header | `.chapter-comment-meta` (flex, trong user wrapper) | `.chapter-comment-header` (flex, trong body) |
| Content position | sibling của `.chapter-comment-user` + `margin-left:46px` | con của `.chapter-comment-body`, không cần margin-left |

### Visual Result

| | Before | After |
|---|---|---|
| Avatar | Nổi giữa dọc item | Ghim trên cùng trái |
| Author + time | Trong `.chapter-comment-user`, có thể lệch | Nằm trong body, baseline-aligned |
| Content | Tách rời, bị dịch `46px` không chắc | Ngay dưới header trong body |
| Layout cảm giác | Vỡ, rộng, không gom lại | Compact, nhìn như normal comment |

---

## Tại Sao Không Đổi novel/show.html

Trang novel/show.html dùng một set class CSS khác hoàn toàn (`comment-item`, `comment-user`, `comment-meta`, `comment-header`, `comment-text` trong `novel_detail.css`). Đây là 2 hệ thống CSS độc lập — fix reader page không ảnh hưởng novel page.

---

## Tại Sao Không Đổi Backend

Đây là lỗi thuần HTML/CSS. Không có thay đổi nào đến:
- `ReaderController.java`
- `CommentService.java`
- `CommentRepository.java`
- `Comment.java` entity
- Bất kỳ route hay endpoint nào
