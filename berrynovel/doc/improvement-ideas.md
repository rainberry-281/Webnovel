# berryNovel — Ý Tưởng Cải Thiện Web

> Dựa trên phân tích toàn bộ source code hiện có. Mỗi ý tưởng được đánh giá theo
> **Độ khó** (Dễ / Trung bình / Khó) và **Tác động** (⭐ → ⭐⭐⭐).

---

## Tính năng hiện có (để tham khảo)

| Tính năng | Trạng thái |
|---|---|
| Đọc truyện, danh sách chương | ✅ |
| Tìm kiếm, lọc theo thể loại / type / progress | ✅ |
| Đăng ký / đăng nhập | ✅ |
| Thư viện (bookshelf) | ✅ |
| Bookmark vị trí đọc (paragraph-level) | ✅ |
| TTS (text-to-speech) | ✅ |
| Đếm lượt đọc + HOT badge | ✅ |
| Comment (novel + chapter) | ✅ |
| Lịch sử đọc (localStorage) | ✅ |
| Admin panel | ✅ |

---

## Nhóm 1 — Trải nghiệm đọc (Reading UX)

### 1.1 Reading Settings Panel ⭐⭐⭐
**Độ khó: Dễ (CSS + localStorage)**

Thêm panel cài đặt đọc truyện có thể lưu vào localStorage:

- **Cỡ chữ**: nhỏ / vừa / lớn (thay `font-size` trên `#chapter-content`)
- **Chủ đề nền**: Trắng / Sepia (`#f3dcc0` đang dùng) / Xám nhạt / Đen (dark mode)
- **Font**: Serif (đang dùng) / Sans-serif
- **Giãn dòng**: hẹp / vừa / rộng

```
Tại sao dễ: Chỉ cần thêm một icon vào sidebar icon list, lưu preferences vào
localStorage, apply class lên body. Không cần backend.
```

---

### 1.2 Thanh tiến trình đọc chương ⭐⭐
**Độ khó: Dễ (CSS + JS)**

Thêm một thanh ngang mỏng cố định ở trên cùng trang, tự động điều chỉnh chiều rộng theo % scroll hiện tại của chapter content.

```js
// Ví dụ logic
window.addEventListener('scroll', () => {
    const percent = window.scrollY / (document.body.scrollHeight - window.innerHeight);
    document.getElementById('read-progress').style.width = (percent * 100) + '%';
});
```

Người dùng nhìn được mình đọc đến đâu trong chương mà không cần nhìn scrollbar.

---

### 1.3 Nút "Tiếp tục đọc" nổi bật hơn ⭐⭐
**Độ khó: Dễ**

Trên trang novel detail, nếu người dùng đã có bookmark (đã đọc đến đoạn nào đó):
- Thêm nút **"Continue reading — Chap X"** nổi bật cạnh nút "Read from beginning"
- Nút này link thẳng đến `?from=bookmark` của chapter đó

Hiện tại bookmark đã có đủ data (`linePosition`, `paragraphKey`), chỉ cần hiển thị nút này nếu user có bookmark.

---

### 1.4 Keyboard Shortcuts ⭐
**Độ khó: Dễ (JS)**

Trên trang reader, thêm phím tắt:
- `←` / `→` hoặc `A` / `D` → chương trước / chương sau
- `T` → toggle TTS panel
- `B` → toggle bookmark
- `Esc` → đóng sidebar (đã có)

Thêm tooltip nhỏ hiển thị khi hover lên nút điều hướng.

---

### 1.5 Auto-scroll (tự động cuộn) ⭐
**Độ khó: Trung bình (JS)**

Thêm chế độ auto-scroll với tốc độ có thể điều chỉnh — rất phổ biến trên các web đọc truyện di động. Người dùng đọc không cần tay cuộn trang.

Có thể tích hợp vào thanh điều khiển hiện có (thêm button vào `rd-side-icons`).

---

## Nhóm 2 — Khám phá & Phân loại (Discovery)

### 2.1 Trang "Đang cập nhật" / "Mới nhất" ⭐⭐
**Độ khó: Dễ (query + template)**

Thêm section trên homepage hoặc một trang riêng hiển thị các truyện có chương mới nhất được đăng gần đây, sắp xếp theo `chapter.createdAt DESC`.

```java
// Repository
List<Novel> findNovelsWithLatestChapters(Pageable pageable);
// hoặc JOIN Chapters ORDER BY chapters.created_at DESC
```

---

### 2.2 Truyện liên quan (Related Novels) ⭐⭐
**Độ khó: Dễ**

Trên trang novel detail, hiển thị 4–6 truyện cùng thể loại (genre) hoặc cùng tác giả.

```java
// Service
List<Novel> findRelatedNovels(Long novelId, List<Genre> genres, int limit);
```

Hoàn toàn phù hợp với data model hiện có (`Genre` entity đã có).

---

### 2.3 Top Novels (Bảng xếp hạng) ⭐⭐⭐
**Độ khó: Dễ**

Thêm trang `/ranking` hiển thị bảng xếp hạng dựa trên `novel.totalReadCount`:
- Top đọc nhiều nhất (tất cả thời gian)
- Top HOT
- Top mới nhất

Data đã có sẵn (`totalReadCount` trong `Novel` entity, `ChapterReadLog` đã track theo thời gian). Chỉ cần thêm query và template.

---

### 2.4 Tags / Nhãn tìm kiếm nhanh ⭐
**Độ khó: Dễ (template)**

Trên trang category/search, thêm các tag nhanh có thể click:
- `Hoàn thành`, `Đang ra`, `Ngôn tình`, `Huyền huyễn`, `Hot`...

Thực chất là shortcut cho bộ lọc đã có — chỉ cần render chúng như chip/tag clickable.

---

## Nhóm 3 — Tương tác Người dùng (Social)

### 3.1 Rating truyện (1–5 sao) ⭐⭐⭐
**Độ khó: Trung bình**

Thêm hệ thống đánh giá sao cho novel:
- Entity `NovelRating` (user + novel + score)
- Hiển thị sao trung bình trên trang detail và card truyện
- Người dùng click để đánh giá

Cần: entity mới, repository, service, endpoint POST, UI.

---

### 3.2 Like comment ⭐
**Độ khó: Trung bình**

Thêm nút 👍 vào comment. Đơn giản nhất là dùng localStorage (không cần login), tương tự reading history. Nếu muốn persist, cần thêm entity `CommentLike`.

---

### 3.3 Reply / Nested Comments ⭐
**Độ khó: Khó**

Cho phép reply vào một comment cụ thể (1 level nesting). Cần thêm `parentComment` field vào `Comment` entity và thay đổi cách render Thymeleaf (recursive hoặc group by parent).

---

### 3.4 Trang User Profile ⭐⭐
**Độ khó: Trung bình**

Tạo trang profile công khai cho user: `/user/{username}` hiển thị:
- Ảnh đại diện, tên
- Số truyện đã bookmark
- Lịch sử comment gần đây (public)
- (Tùy chọn) danh sách truyện yêu thích

---

## Nhóm 4 — Thư viện & Theo dõi (Library)

### 4.1 Trạng thái đọc trong thư viện ⭐⭐⭐
**Độ khó: Dễ–Trung bình**

Hiện tại `Bookshelf` chỉ lưu `user + novel + savedAt`. Thêm field `readStatus`:

```java
public enum ReadStatus { READING, COMPLETED, PLAN_TO_READ, ON_HOLD }
```

Cho phép user phân loại truyện trong thư viện thành các danh mục. Đây là tính năng quen thuộc trên MyAnimeList / NovelUpdates.

---

### 4.2 Lịch sử đọc phía server ⭐⭐
**Độ khó: Trung bình**

Hiện tại lịch sử đọc chỉ lưu trong `localStorage` — mất khi xóa dữ liệu trình duyệt.

`ChapterReadLog` entity đã tồn tại và track lượt đọc — có thể mở rộng để tạo trang "Lịch sử đọc" cho user đăng nhập:
- Hiển thị các chương đã đọc gần đây
- Sort theo thời gian
- Group theo novel

---

### 4.3 Thông báo chương mới ⭐⭐
**Độ khó: Khó**

Khi có chương mới của truyện đã bookmark, thông báo cho user.

**Cách đơn giản nhất (không cần WebSocket):**
- Khi user vào trang, check xem các novel trong bookshelf có chương mới hơn lần đăng nhập cuối không
- Hiển thị badge đỏ trên icon thư viện

---

## Nhóm 5 — Admin & Quản lý

### 5.1 Dashboard Analytics ⭐⭐⭐
**Độ khó: Trung bình**

Dashboard hiển thị biểu đồ đơn giản:
- Lượt đọc theo ngày/tuần (data có sẵn từ `ChapterReadLog`)
- Top 5 truyện được đọc nhiều nhất
- Số user đăng ký theo thời gian
- Tổng comment trong tuần

Dùng Chart.js (CDN, nhẹ) để render biểu đồ.

---

### 5.2 Comment Moderation ⭐⭐
**Độ khó: Dễ**

Trang admin quản lý comment:
- Xem tất cả comment mới nhất
- Xóa hàng loạt
- Lọc theo novel / user

Hiện tại admin đã có `deleteComment` qua controller, chỉ cần thêm giao diện admin list.

---

### 5.3 Upload chương hàng loạt ⭐⭐
**Độ khó: Trung bình**

Tính năng import nhiều chương cùng lúc từ file `.txt` hoặc `.docx`, tự động tách chương theo heading/separator.

---

## Nhóm 6 — Performance & Kỹ thuật

### 6.1 Lazy load ảnh bìa ⭐
**Độ khó: Dễ (1 attribute)**

Thêm `loading="lazy"` vào tất cả `<img>` của novel cover trên trang listing/homepage. Giảm load time ban đầu đáng kể khi có nhiều truyện.

```html
<!-- Thay: -->
<img th:src="..." alt="...">
<!-- Bằng: -->
<img th:src="..." alt="..." loading="lazy">
```

---

### 6.2 Phân trang comment ⭐⭐
**Độ khó: Dễ**

Hiện tại comment load toàn bộ. Khi comment nhiều lên sẽ chậm. Thêm `Pageable` vào repository:

```java
Page<Comment> findByNovel_IdOrderByCreatedAtDesc(Long novelId, Pageable pageable);
```

---

### 6.3 Chapter content caching ⭐
**Độ khó: Trung bình (Spring Cache)**

Nội dung chương không thay đổi thường xuyên — có thể cache bằng `@Cacheable` của Spring:

```java
@Cacheable("chapters")
public Chapter getChapterById(Long id) { ... }
```

---

### 6.4 SEO cơ bản ⭐⭐
**Độ khó: Dễ (template)**

Thêm vào `<head>` của mỗi trang:
- `<meta name="description">` với mô tả novel/chapter
- `<meta property="og:image">` với ảnh bìa novel
- `<title>` dynamic thay vì luôn là "Chapter" hay "berryNovel"

```html
<!-- Ví dụ cho reader page -->
<title th:text="${chapter.title + ' — ' + novel.title + ' | berryNovel'}"></title>
<meta name="description" th:content="${novel.description != null ? #strings.abbreviate(novel.description, 160) : ''}">
```

---

## Nhóm 7 — UI/UX nhỏ nhưng có giá trị

### 7.1 Xác nhận khi xóa comment ⭐
**Độ khó: Dễ (JS)**

Thêm `confirm()` trước khi submit form xóa comment, tránh xóa nhầm.

```html
<form onsubmit="return confirm('Xóa comment này?')">
```

---

### 7.2 Highlight tìm kiếm ⭐
**Độ khó: Dễ (template)**

Trên trang kết quả tìm kiếm, highlight từ khóa trong title/author.

---

### 7.3 Sticky navigation trên reader ⭐
**Độ khó: Dễ (CSS)**

Hiện tại `.rd-side-icons` đã `position: fixed`. Xem xét thêm một mini nav bar nhỏ ở trên cùng khi cuộn xuống (hiển thị title chương, nút prev/next).

---

### 7.4 Skeleton loading ⭐
**Độ khó: Dễ (CSS)**

Khi trang đang load, hiển thị skeleton placeholder (hình xám giống card) thay vì trang trắng.

---

## Tổng hợp theo mức độ ưu tiên

### 🔴 Nên làm ngay (Dễ + Tác động cao)

| # | Tính năng | Vì sao |
|---|---|---|
| 1.1 | Reading settings (theme, font size) | UX thiết yếu cho web đọc truyện |
| 1.2 | Reading progress bar | Feedback trực quan khi đọc |
| 2.1 | Trang "Mới cập nhật" | Giúp user khám phá nội dung mới |
| 2.3 | Bảng xếp hạng | Data đã có, chỉ cần query + template |
| 6.1 | Lazy load ảnh | 1 attribute, giảm load time ngay |
| 6.4 | SEO meta tags | Chỉ sửa template, tác động lớn |

### 🟡 Nên lên kế hoạch (Trung bình)

| # | Tính năng | Vì sao |
|---|---|---|
| 3.1 | Rating sao | Tăng tương tác, dữ liệu phong phú |
| 4.1 | Trạng thái đọc trong thư viện | Trải nghiệm người dùng rõ ràng hơn |
| 4.2 | Lịch sử đọc phía server | Backup cho localStorage |
| 5.1 | Analytics dashboard | Hữu ích cho admin |

### 🟢 Tương lai (Khó / phức tạp hơn)

| # | Tính năng | Ghi chú |
|---|---|---|
| 3.3 | Nested comments | Cần refactor Comment entity + render |
| 4.3 | Thông báo chương mới | Cần WebSocket hoặc polling |
| 1.5 | Auto-scroll | Edge cases nhiều |
