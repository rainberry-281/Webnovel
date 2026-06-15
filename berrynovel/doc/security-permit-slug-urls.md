# Cập nhật Security Permit cho URL Slug – Novel Detail & Reader

## 1. Vấn đề

Sau khi đổi URL novel detail và reader từ dạng ID sang dạng **slug**, hệ thống sử dụng hai pattern mới:

| Trang | URL cũ (legacy) | URL mới (slug) |
|-------|----------------|----------------|
| Novel Detail | `/novel/{id}` | `/{id}-{slug-title}` |
| Reader | `/reader/{novelID}/{chapterID}` | `/{id}-{slug-title}/c{id}-{slug-title}` |

Ví dụ thực tế:
- Novel: `/42-toan-thu-than-nong-dan`
- Reader: `/42-toan-thu-than-nong-dan/c101-chuong-1-khai-dau`

**Format slug:**
- Novel slug: `^(\d+)-[a-z0-9-]+$` → `{novelId}-{title-slug}`
- Chapter slug: `^c(\d+)-[a-z0-9-]+$` → `c{chapterId}-{title-slug}`

Spring Security **không nhận** AntPathMatcher cho pattern dạng này vì có ký tự số ở đầu path, nên phải dùng **`RegexRequestMatcher`**.

---

## 2. Hiện trạng trong `SecurityConfiguration.java`

```java
// Dòng 70–75: Permit cũ (vẫn giữ legacy route)
.requestMatchers("/", "/home", "/login", "/register", "/novel/**",
                 "/reader/**", "/category/**",
                 "/client/**", "/search/**",
                 "/css/**", "/js/**",
                 "/images/**", "/vendor/**")
.permitAll()

// Dòng 76–80: Permit API audio (không đổi)
.requestMatchers(
    AntPathRequestMatcher.antMatcher("/api/chapter/*/audio-status"),
    AntPathRequestMatcher.antMatcher("/audio/**"))
.permitAll()

// Dòng 81–85: Permit slug mới (ĐÃ CÓ)
.requestMatchers(RegexRequestMatcher.regexMatcher("^/\\d+-[a-z0-9-]+$"))
.permitAll()
.requestMatchers(RegexRequestMatcher.regexMatcher("^/\\d+-[a-z0-9-]+/c\\d+-[a-z0-9-]+$"))
.permitAll()
```

> ✅ **Regex permit đã tồn tại** ở dòng 81–85 — hai pattern slug đã được `permitAll()`.

---

## 3. Phân tích chi tiết từng Route

### 3.1 Novel Detail

| Loại | Route | Permit hiện tại |
|------|-------|----------------|
| Legacy (redirect) | `GET /novel/{id}` | ✅ `/novel/**` – permitAll |
| Slug (canonical) | `GET /{id}-{slug}` | ✅ Regex `^/\d+-[a-z0-9-]+$` – permitAll |

**Lưu ý:** Controller `ClientNovelController` xử lý `/novel/{id}` bằng `redirectLegacyNovelDetailPage()` → 301 redirect sang slug URL. Route này cần `permitAll` để Guest có thể redirect mà không bị chặn.

### 3.2 Reader

| Loại | Route | Permit hiện tại |
|------|-------|----------------|
| Legacy (redirect) | `GET /reader/{novelID}/{chapterID}` | ✅ `/reader/**` – permitAll |
| Slug (canonical) | `GET /{novelSlug}/c{chapterId}-{slug}` | ✅ Regex `^/\d+-[a-z0-9-]+/c\d+-[a-z0-9-]+$` – permitAll |

**Lưu ý quan trọng về Regex:** Pattern hiện tại chỉ permit URL **không có query string**. Spring Security đánh giá path trước khi query string, nên `?page=2` không ảnh hưởng.

---

## 4. Các trường hợp có thể bị thiếu Permit

### 4.1 Query param `?from=...` trên Novel Detail

URL: `/{novelSlug}?from=category`

Regex hiện tại `^/\d+-[a-z0-9-]+$` chỉ match path — **không bao gồm query string**. Spring Security áp dụng regex trên **request URI (path only)**, nên `?from=category` **không gây vấn đề**.

✅ Không cần sửa.

### 4.2 Comment POST trên Novel Detail (User only)

```
POST /{novelSlug}/comment
POST /{novelSlug}/comment/{commentId}/delete
```

Các route này **yêu cầu đăng nhập** (controller kiểm tra `authentication`). Regex hiện tại chỉ permit `GET`, nhưng thực ra `RegexRequestMatcher` không phân biệt method — nó permit **tất cả method** trên pattern đó.

> [!WARNING]
> `RegexRequestMatcher.regexMatcher("^/\\d+-[a-z0-9-]+$")` permit **tất cả HTTP method** (GET, POST, DELETE...) khớp pattern đó.
>
> Điều này có nghĩa là `POST /{novelSlug}/comment` cũng bị permitAll, nhưng controller tự kiểm tra authentication bên trong và redirect về `/login` nếu chưa đăng nhập → **Hoạt động đúng nhưng bảo mật phụ thuộc vào controller, không phải Security layer.**

### 4.3 Chapter Comment POST (User only)

```
POST /{novelSlug}/{chapterSlug}/comments
```

Tương tự trên — pattern `^/\d+-[a-z0-9-]+/c\d+-[a-z0-9-]+$` permit cả POST này. Controller kiểm tra auth bên trong.

---

## 5. Cách sửa đúng trong `SecurityConfiguration.java`

### Phương án A (Hiện tại – Đơn giản, đang hoạt động)

Giữ nguyên regex `permitAll()` cho toàn bộ pattern slug. Bảo vệ POST endpoints bằng controller logic. **Phù hợp nếu bạn không muốn phức tạp hóa Security config.**

### Phương án B (Chặt chẽ hơn – Phân biệt GET/POST)

Nếu muốn tầng Security bảo vệ POST endpoints, sửa như sau:

```java
// Chỉ permit GET cho slug URLs
.requestMatchers(
    RegexRequestMatcher.regexMatcher("GET", "^/\\d+-[a-z0-9-]+$"),
    RegexRequestMatcher.regexMatcher("GET", "^/\\d+-[a-z0-9-]+/c\\d+-[a-z0-9-]+$"))
.permitAll()
```

> Khi đó các POST request lên slug URL sẽ yêu cầu authentication ở tầng Security.

---

## 6. Code chỉnh sửa đề xuất (Phương án B)

Trong `SecurityConfiguration.java`, thay thế đoạn từ **dòng 81–85**:

```java
// TRƯỚC (permit tất cả method):
.requestMatchers(RegexRequestMatcher.regexMatcher("^/\\d+-[a-z0-9-]+$"))
.permitAll()
.requestMatchers(RegexRequestMatcher
        .regexMatcher("^/\\d+-[a-z0-9-]+/c\\d+-[a-z0-9-]+$"))
.permitAll()
```

```java
// SAU (chỉ permit GET):
.requestMatchers(
    RegexRequestMatcher.regexMatcher("GET", "^/\\d+-[a-z0-9-]+$"),
    RegexRequestMatcher.regexMatcher("GET", "^/\\d+-[a-z0-9-]+/c\\d+-[a-z0-9-]+$"))
.permitAll()
```

---

## 7. Kiểm tra sau khi thay đổi

| Test case | Kỳ vọng | Cách kiểm tra |
|-----------|---------|--------------|
| Guest vào trang chủ `/home` | ✅ 200 OK | Mở browser không đăng nhập |
| Guest vào `/42-toan-thu-than-nong-dan` | ✅ 200 OK | Truy cập trực tiếp slug |
| Guest vào `/novel/42` | ✅ 301 → slug | Redirect sang canonical |
| Guest vào chapter slug | ✅ 200 OK | Truy cập slug chương |
| Guest vào `/reader/42/101` | ✅ 301 → slug | Redirect sang canonical |
| Guest POST comment | Tùy phương án | A: redirect login, B: 403 |
| User đăng nhập truy cập `/bookshelf` | ✅ 200 OK | Đăng nhập trước |
| Không đăng nhập vào `/bookshelf` | ✅ redirect `/login` | Không đăng nhập |
| Admin vào `/admin` | ✅ 200 OK | Đăng nhập admin |
| User thường vào `/admin` | ✅ 403 | Đăng nhập user thường |

---

## 8. Tóm tắt trạng thái hiện tại

| Route | Phương thức | Permit? | Ghi chú |
|-------|------------|---------|---------|
| `/` | GET | ✅ | permitAll |
| `/home` | GET | ✅ | permitAll |
| `/login`, `/register` | GET, POST | ✅ | permitAll |
| `/novel/**` | ALL | ✅ | permitAll (legacy redirect) |
| `/reader/**` | ALL | ✅ | permitAll (legacy redirect) |
| `/category/**`, `/search/**` | ALL | ✅ | permitAll |
| `/{id}-{slug}` | ALL | ✅ | RegexMatcher – permitAll |
| `/{id}-{slug}/c{id}-{slug}` | ALL | ✅ | RegexMatcher – permitAll |
| `/css/**`, `/js/**`, `/images/**` | ALL | ✅ | Static resources |
| `/bookshelf/**`, `/library/**` | ALL | 🔒 | anyRequest().authenticated() |
| `/profile/**` | ALL | 🔒 | anyRequest().authenticated() |
| `/admin/**` | ALL | 🔑 | hasRole("ADMIN") |
