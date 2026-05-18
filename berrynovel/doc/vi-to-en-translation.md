# Vietnamese → English Translation Task

> **Project:** berrynovel (Spring Boot + Thymeleaf + MySQL)
> **Scope:** All user-facing text, HTML labels, tooltips, comments, error messages,
> placeholder text, button titles, and JS inline strings.
> **DO NOT translate:** variable names, function names, routes, DB column names,
> CSS class names, IDs, Thymeleaf expressions, dynamic data from DB
> (novel.title, chapter.title, novel.author, chapter.content, genre.name).

---

## Prompt

```
You are working on my source code. Your task is to replace all Vietnamese
user-facing text and Vietnamese comments in the source code with clear English.

Scope:
- Search the entire project source code.
- Replace all Vietnamese text in HTML, Thymeleaf templates, JavaScript, Java,
  CSS comments, error messages, button titles, labels, placeholders, alerts,
  console messages, tooltips, modal text, validation messages, and comments.
- Do NOT change variable names, function names, endpoint URLs, database column
  names, class names, CSS class names, IDs, or API contracts unless they are
  purely display text.
- Do NOT change business logic.
- Do NOT refactor unrelated code.
- Do NOT translate dynamic data coming from the database, such as novel.title,
  chapter.title, author names, or chapter.content.

Important translation rules:
1. Keep technical meaning natural, not word-by-word.
2. Use consistent English wording:
   - "Bỏ bookmark"            → "Remove bookmark"
   - "Bookmark chapter"       → "Bookmark chapter"
   - "Ảnh bìa"                → "Cover image"
   - "Vị trí bookmark của bạn"→ "Your bookmark position"
   - "Chương"                 → "Chapter"
   - "Truyện"                 → "Novel"
   - "Đăng nhập"              → "Log in"
   - "Đăng ký"                → "Sign up"
   - "Tìm kiếm"               → "Search"
   - "Cập nhật"               → "Update"
   - "Xóa"                    → "Delete"
   - "Lưu"                    → "Save"
   - "Hủy"                    → "Cancel"
   - "Quay lại"               → "Back"
   - "Tác giả"                → "Author"
   - "Tên truyện"             → "Novel title"
   - "Giới thiệu"             → "Description"
   - "Bình luận"              → "Comments"
   - "Viết bình luận"         → "Write a comment"
   - "Gửi bình luận"          → "Post comment"
   - "Chưa có bình luận nào"  → "No comments yet."
   - "Khám phá ngay"          → "Explore now"
   - "Đọc từ đầu"             → "Read from beginning"
   - "Chương mới nhất"        → "Latest chapter"
   - "Danh sách chương"       → "Chapter list"
   - "Tài khoản"              → "Account"
   - "Tên truyện" (table)     → "Novel title"
   - "Mới nhất" (table)       → "Latest chapter"
   - "Chưa có truyện..."      → "No novels in your library yet."
   - "Chưa có chương"         → "No chapters yet"
   - "Danh sách đánh dấu"     → "Bookmarks"
   - "Chưa có chapter nào được bookmark" → "No bookmarked chapters yet."
   - "Hồ sơ cá nhân"          → "Edit profile"
   - "Thế giới truyện chữ"    → "Your world of stories"
   - "Thêm vào tủ sách"       → "Add to library"
   - "Bỏ khỏi tủ sách"        → "Remove from library"
   - "Đăng nhập để thêm vào thư viện" → "Log in to add to your library"
   - "Xem thêm"               → "Show more"
   - "Thu gọn"                → "Show less"
   - "Xóa đã chọn"            → "Delete selected"
   - "Chưa có truyện trong thư viện" → "No novels in your library yet."
   - "Tìm truyện"             → "Search novels"

3. Preserve Thymeleaf syntax exactly.
4. Preserve JavaScript logic exactly.
5. Preserve HTML structure exactly.
6. Preserve routes exactly.
7. Preserve CSRF handling exactly.
8. Preserve all data-* attributes exactly.
9. Do not translate values that are part of code identifiers.

After editing:
- Show me a list of files changed.
- Show examples of Vietnamese text that was replaced.
- Confirm that no logic, routes, database fields, or identifiers were changed.
- Run a quick search to confirm no Vietnamese user-facing text remains.
```

---

## Files to Change

### CLIENT — Thymeleaf Templates

---

#### 1. `templates/client/homepage/show.html`

| Location | Vietnamese | English |
|---|---|---|
| `<title>` | `berryNovel - Thế giới truyện chữ` | `berryNovel - Your World of Stories` |
| `lang` attribute | `lang="vi"` | `lang="en"` |
| Button text | `Khám phá ngay` | `Explore now` |
| `alt` attribute | `Tên truyện` (×3) | `Novel cover` |
| `th:text` overlay | `'Tác giả: ' + ${novel.author}` | `'Author: ' + ${novel.author}` |

**Changed lines (examples):**
```diff
-<title>berryNovel - Thế giới truyện chữ</title>
+<title>berryNovel - Your World of Stories</title>

-    Khám phá ngay
+    Explore now

-alt="Tên truyện"
+alt="Novel cover"

-th:text="'Tác giả: ' + ${novel.author}"
+th:text="'Author: ' + ${novel.author}"
```

---

#### 2. `templates/client/layout/navigation.html`

| Location | Vietnamese | English |
|---|---|---|
| Search input placeholder | `Tìm truyện` | `Search novels` |

```diff
-placeholder="Tìm truyện"
+placeholder="Search novels"
```

---

#### 3. `templates/client/novel/show.html`

| Location | Vietnamese | English |
|---|---|---|
| `<title>` | `berryNovel - Thế giới truyện chữ` | `berryNovel - Your World of Stories` |
| `lang` attribute | `lang="vi"` | `lang="en"` |
| `alt` attribute | `Ảnh bìa` | `Cover image` |
| Bookshelf button title | `'Bỏ khỏi tủ sách' : 'Thêm vào tủ sách'` | `'Remove from library' : 'Add to library'` |
| Login link title | `Đăng nhập để thêm vào thư viện` | `Log in to add to your library` |
| Read button | `Đọc từ đầu` | `Read from beginning` |
| Latest chapter button | `Chương mới nhất` | `Latest chapter` |
| Section header | `Giới thiệu` | `Description` |
| Toggle button | `Xem thêm` (×2) | `Show more` |
| Section header | `Danh sách chương` | `Chapter list` |
| Section header | `Bình luận` | `Comments` |
| Textarea placeholder | `Viết bình luận...` | `Write a comment...` |
| Submit button | `Gửi bình luận` | `Post comment` |
| Login link text | `Đăng nhập` (in comment prompt) | `Log in` |
| Login prompt sentence | `để bình luận.` | `to comment.` |
| Empty state | `Chưa có bình luận nào.` | `No comments yet.` |
| Delete button | `Xóa` | `Delete` |
| JS `btn.innerText` | `"Xem thêm"` (×2), `"Thu gọn"` | `"Show more"` (×2), `"Show less"` |
| `alt` attr overlay | `Tên truyện` | `Novel cover` |
| Author label | `'Tác giả: ' + ${novel.author}` | `'Author: ' + ${novel.author}` |

```diff
-alt="Ảnh bìa"
+alt="Cover image"

-th:title="${inBookshelf} ? 'Bỏ khỏi tủ sách' : 'Thêm vào tủ sách'"
+th:title="${inBookshelf} ? 'Remove from library' : 'Add to library'"

-title="Đăng nhập để thêm vào thư viện"
+title="Log in to add to your library"

-    Đọc từ đầu
+    Read from beginning

-    <i class="fa-solid fa-bolt me-1"></i> Chương mới nhất
+    <i class="fa-solid fa-bolt me-1"></i> Latest chapter

-<h4 class="fw-bold mb-4">Giới thiệu</h4>
+<h4 class="fw-bold mb-4">Description</h4>

-<button id="toggleBtn">Xem thêm</button>
+<button id="toggleBtn">Show more</button>

-    Danh sách chương
+    Chapter list

-    Bình luận
+    Comments

-placeholder="Viết bình luận..." required>
+placeholder="Write a comment..." required>

-    Gửi bình luận
+    Post comment

-<a th:href="@{/login}">Đăng nhập</a> để bình luận.
+<a th:href="@{/login}">Log in</a> to comment.

-    Chưa có bình luận nào.
+    No comments yet.

-<button type="submit" class="comment-delete-btn">Xóa</button>
+<button type="submit" class="comment-delete-btn">Delete</button>

-btn.innerText = "Xem thêm";   // (×2)
+btn.innerText = "Show more";

-btn.innerText = "Thu gọn";
+btn.innerText = "Show less";
```

---

#### 4. `templates/client/library/bookshelf.html`

| Location | Vietnamese | English |
|---|---|---|
| `lang` attribute | `lang="vi"` | `lang="en"` |
| Section header | `Tài khoản` | `Account` |
| Delete button | `Xóa đã chọn` | `Delete selected` |
| Table header | `Tên truyện` | `Novel title` |
| Table header | `Mới nhất` | `Latest chapter` |
| Empty state | `Chưa có truyện trong thư viện.` | `No novels in your library yet.` |
| Empty chapter cell | `Chưa có chương` | `No chapters yet` |

```diff
-<h4 class="section-name">Tài khoản</h4>
+<h4 class="section-name">Account</h4>

-<button class="btn btn-danger rounded-pill fw-bold px-3" type="submit">Xóa đã chọn</button>
+<button class="btn btn-danger rounded-pill fw-bold px-3" type="submit">Delete selected</button>

-<th class="col-10 col-md-8">Tên truyện</th>
+<th class="col-10 col-md-8">Novel title</th>

-<th class="none table-cell-m col-md-4">Mới nhất</th>
+<th class="none table-cell-m col-md-4">Latest chapter</th>

-<td colspan="3" class="text-center py-4">Chưa có truyện trong thư viện.</td>
+<td colspan="3" class="text-center py-4">No novels in your library yet.</td>

-<span th:if="${item.newestChapter == null}">Chưa có chương</span>
+<span th:if="${item.newestChapter == null}">No chapters yet</span>
```

---

#### 5. `templates/client/library/bookmark.html`

| Location | Vietnamese | English |
|---|---|---|
| `lang` attribute (if set to `vi`) | `lang="vi"` | `lang="en"` |
| Section header | `Danh sách đánh dấu` | `Bookmarks` |
| Empty state | `Chưa có chapter nào được bookmark.` | `No bookmarked chapters yet.` |

```diff
-<span class="sect-title">Danh sách đánh dấu</span>
+<span class="sect-title">Bookmarks</span>

-<p class="text-muted" th:if="${#lists.isEmpty(bookmarkItems)}">Chưa có chapter nào được bookmark.</p>
+<p class="text-muted" th:if="${#lists.isEmpty(bookmarkItems)}">No bookmarked chapters yet.</p>
```

---

#### 6. `templates/client/reader/show.html`

| Location | Vietnamese | English |
|---|---|---|
| Bookmark button title | `'Bỏ bookmark chapter' : 'Bookmark chapter'` | `'Remove chapter bookmark' : 'Bookmark chapter'` |
| Cover image alt | `Ảnh bìa` | `Cover image` |
| JS comment | `// nếu click vào sidebar thì không toggle` | `// Do not toggle when clicking inside the sidebar.` |

```diff
-th:title="${isBookmarked} ? 'Bỏ bookmark chapter' : 'Bookmark chapter'"
+th:title="${isBookmarked} ? 'Remove chapter bookmark' : 'Bookmark chapter'"

-alt="Ảnh bìa"
+alt="Cover image"

-// nếu click vào sidebar thì không toggle
+// Do not toggle when clicking inside the sidebar.
```

---

#### 7. `templates/client/profile/edit.html`

| Location | Vietnamese | English |
|---|---|---|
| Card header | `Hồ sơ cá nhân` | `Edit profile` |
| HTML comment | `<!-- lưu ảnh cũ -->` | `<!-- keep existing image -->` |

```diff
-    Hồ sơ cá nhân
+    Edit profile

-<!-- lưu ảnh cũ -->
+<!-- keep existing image -->
```

---

#### 8. `templates/client/category/show.html`

| Location | Vietnamese | English |
|---|---|---|
| `<title>` | `berryNovel - Thế giới truyện chữ` | `berryNovel - Your World of Stories` |
| `lang` attribute | `lang="vi"` | `lang="en"` |
| Author overlay text | `'Tác giả: ' + ${novel.author}` | `'Author: ' + ${novel.author}` |

```diff
-<title>berryNovel - Thế giới truyện chữ</title>
+<title>berryNovel - Your World of Stories</title>

-th:text="'Tác giả: ' + ${novel.author}"
+th:text="'Author: ' + ${novel.author}"
```

---

### ADMIN — Thymeleaf Templates

---

#### 9. `templates/admin/novel/create.html`

| Location | Vietnamese | English |
|---|---|---|
| JS comment | `// sync data trước khi submit` | `// Sync editor data before form submit.` |

```diff
-// sync data trước khi submit
+// Sync editor data before form submit.
```

---

#### 10. `templates/admin/novel/update.html`

| Location | Vietnamese | English |
|---|---|---|
| JS comment | `// sync dữ liệu trước khi submit` | `// Sync editor data before form submit.` |

```diff
-// sync dữ liệu trước khi submit
+// Sync editor data before form submit.
```

---

#### 11. `templates/admin/chapter/create.html`

| Location | Vietnamese | English |
|---|---|---|
| CSS comment | `/* kích thước tối đa */` | `/* maximum image size */` |
| JS comment | `// sync dữ liệu trước khi submit form` | `// Sync editor data before form submit.` |

```diff
-/* kích thước tối đa */
+/* maximum image size */

-// sync dữ liệu trước khi submit form
+// Sync editor data before form submit.
```

---

#### 12. `templates/admin/chapter/update.html`

*(Read and apply same pattern as `create.html` — sync comment and any CSS comments)*

---

#### 13. `templates/admin/genre/show.html`

*(Read and translate any Vietnamese labels, table headers, button text)*

---

#### 14. `templates/admin/user/create.html`

*(Read and translate any Vietnamese labels, validation messages)*

---

#### 15. `templates/admin/user/update.html`

*(Read and translate any Vietnamese labels, validation messages)*

---

#### 16. `templates/admin/dashboard/show.html`

*(Read and translate any Vietnamese section headers or stat labels)*

---

### CLIENT — Static JavaScript

---

#### 17. `static/client/js/navitigation.js`

*(Read and translate any Vietnamese inline strings or comments)*

---

### CLIENT — CSS Files

*(CSS files in the project contain minimal or no Vietnamese — verify and translate any Vietnamese in CSS comments)*

| File | Check |
|---|---|
| `static/client/css/chapter.css` | Vietnamese in comments from mục 5.9 spec |
| `static/client/css/library/bookmark.css` | Any Vietnamese comments |
| `static/client/css/library/bookshelf.css` | Any Vietnamese comments |
| `static/admin/css/chapter/addchapter.css` | Any Vietnamese comments |
| `static/admin/css/sidebar.js` | Any Vietnamese comments |

---

### JAVA — Service / Validator Layer

---

#### 18. `service/LibraryService.java`

| Location | Vietnamese | English |
|---|---|---|
| Exception message | `"User not found"` | Already English ✓ |
| Exception message | `"Chapter not found"` | Already English ✓ |
| Exception message | `"Chapter does not belong to novel"` | Already English ✓ |
| Log divider string | `LOG_DIVIDER` comments (if any) | Already English ✓ |

> **Note:** `LibraryService.java` error messages are already in English. Verify all other `.java` service files below.

---

#### 19. `service/Validator/RegisterValidator.java`

*(Check for Vietnamese validation error messages — translate to English)*

---

#### 20. `service/Validator/StrongPasswordValidator.java`

*(Check for Vietnamese validation error messages — translate to English)*

---

#### 21. `service/NovelService.java`

*(Check for Vietnamese exception messages or log comments — translate)*

---

#### 22. `service/UserService.java`

*(Check for Vietnamese exception messages or comments — translate)*

---

#### 23. `service/CommentService.java`

*(Check for Vietnamese exception messages or comments — translate)*

---

#### 24. `service/ImageService.java`

*(Check for Vietnamese error strings or comments — translate)*

---

## Summary Table — All Files to Change

| # | File (relative to `src/main/`) | Change Type | Count |
|---|---|---|---|
| 1 | `resources/templates/client/homepage/show.html` | Labels, title, alt, button text, author prefix | 6 |
| 2 | `resources/templates/client/layout/navigation.html` | Placeholder | 1 |
| 3 | `resources/templates/client/novel/show.html` | Labels, buttons, tooltips, JS strings, empty states | 15+ |
| 4 | `resources/templates/client/library/bookshelf.html` | Section headers, table headers, empty states, button | 6 |
| 5 | `resources/templates/client/library/bookmark.html` | Section header, empty state | 2 |
| 6 | `resources/templates/client/reader/show.html` | Tooltip, alt, JS comment | 3 |
| 7 | `resources/templates/client/profile/edit.html` | Card header, HTML comment | 2 |
| 8 | `resources/templates/client/category/show.html` | Title, lang, author prefix | 3 |
| 9 | `resources/templates/admin/novel/create.html` | JS comment | 1 |
| 10 | `resources/templates/admin/novel/update.html` | JS comment | 1 |
| 11 | `resources/templates/admin/chapter/create.html` | CSS comment, JS comment | 2 |
| 12 | `resources/templates/admin/chapter/update.html` | JS comment (verify) | 1 |
| 13 | `resources/templates/admin/genre/show.html` | Any labels (verify) | TBD |
| 14 | `resources/templates/admin/user/create.html` | Any labels (verify) | TBD |
| 15 | `resources/templates/admin/user/update.html` | Any labels (verify) | TBD |
| 16 | `resources/templates/admin/dashboard/show.html` | Any labels (verify) | TBD |
| 17 | `resources/static/client/js/navitigation.js` | Any inline strings (verify) | TBD |
| 18 | `resources/static/client/css/chapter.css` | CSS comments (verify) | TBD |
| 19 | `java/.../service/Validator/RegisterValidator.java` | Validation messages (verify) | TBD |
| 20 | `java/.../service/Validator/StrongPasswordValidator.java` | Validation messages (verify) | TBD |
| 21 | `java/.../service/NovelService.java` | Exception messages, comments (verify) | TBD |
| 22 | `java/.../service/UserService.java` | Exception messages, comments (verify) | TBD |
| 23 | `java/.../service/CommentService.java` | Exception messages, comments (verify) | TBD |
| 24 | `java/.../service/ImageService.java` | Exception messages, comments (verify) | TBD |

---

## Files NOT to Change

| File | Reason |
|---|---|
| `static/vendor/**` | Third-party libraries (Bootstrap, FontAwesome, CKEditor) |
| `application.properties` | Config keys and values — not user-facing |
| `domain/*.java` | Entity field names, DB column annotations |
| `repository/*.java` | Query method names |
| `controller/**` | Log messages (internal only, but translate if exposed to users) |
| `*.html` `th:text="${novel.title}"` | Dynamic DB content — never translated |
| `*.html` `th:text="${chapter.title}"` | Dynamic DB content — never translated |
| `*.html` `th:text="${novel.author}"` | Dynamic DB content — never translated |

---

## Do NOT Translate These Patterns

```html
<!-- Dynamic DB data — leave as-is -->
th:text="${novel.title}"
th:text="${chapter.title}"
th:text="${novel.author}"
th:text="${comment.content}"
th:text="${genre.name}"
th:text="${user.username}"
th:text="${user.fullName}"

<!-- Thymeleaf expressions — leave as-is -->
th:href="@{/reader/{novelId}/{chapterId}(...)}"
th:action="@{/bookshelf/bookmark/toggle/{novelId}/{chapterId}(...)}"
th:name="${_csrf.parameterName}"
th:value="${_csrf.token}"

<!-- CSS class names, IDs — leave as-is -->
class="bookmark-toggle-form"
id="chapter-content"
id="rd-info-btn"
```

---

## After-Edit Verification Checklist

- [ ] Run: `grep -rn --include="*.html" --include="*.java" --include="*.js" "[À-ỹ]" src/main/` → zero results
- [ ] All Thymeleaf `sec:authorize` expressions intact
- [ ] All CSRF hidden inputs intact
- [ ] All `th:href`, `th:action` routes unchanged
- [ ] All `data-*` attributes unchanged
- [ ] `lang="en"` on all updated HTML pages
- [ ] No dynamic DB fields translated
- [ ] Build passes: `./mvnw compile` without errors
