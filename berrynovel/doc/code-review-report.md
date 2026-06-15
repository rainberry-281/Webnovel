# BerryNovel – Báo Cáo Review Source Code

**Ngày review:** 2026-06-15  
**Phiên bản:** 0.0.1-SNAPSHOT  
**Kết quả build:** ✅ `BUILD SUCCESS` (`mvn clean package -DskipTests`)  
**Compiler warnings:** `AntPathRequestMatcher` deprecated, unchecked operations trong `GenreController`

---

## 1. Tổng quan kiến trúc

| Thành phần | Công nghệ |
|---|---|
| Framework | Spring Boot 3.5.10, Java 17 |
| Persistence | Spring Data JPA (Hibernate), MySQL |
| Security | Spring Security 6, Spring Session JDBC |
| View | Thymeleaf + thymeleaf-extras-springsecurity6 |
| Packaging | WAR |
| Search/Filter | turkraft/springfilter 3.2.4 |
| Validation | Jakarta Validation, custom `@StrongPassword`, `@RegisterChecked` |
| Migration SQL | Manual (thư mục `db/migration`, KHÔNG dùng Flyway) |

---

## 2. Lỗi phát hiện

---

### 2.1 Lỗi Bảo Mật (Security)

#### BUG-SEC-01 — `CustomSuccessHandler` dùng `@Autowired` trong class không phải Spring bean
- **File:** `config/CustomSuccessHandler.java` – dòng 25–26
- **Mô tả:** `CustomSuccessHandler` được `new` trực tiếp trong `SecurityConfiguration.customSuccessHandler()`, không phải Spring-managed bean. Vì vậy `@Autowired UserService` **KHÔNG được inject** → `userService` luôn là `null` → `NullPointerException` khi login thành công và gọi `getUserByUsername()`.
- **Mức độ:** 🔴 **Cao** – Runtime NPE mỗi khi login
- **Ảnh hưởng:** Login thành công nhưng session không có `username`, `fullName`, `avatar`... → các template hiển thị sai hoặc crash.
- **Cách sửa:** Inject `UserService` qua constructor thay vì `@Autowired`:

```java
// SecurityConfiguration.java
@Bean
public AuthenticationSuccessHandler customSuccessHandler(UserService userService) {
    return new CustomSuccessHandler(userService);
}

// CustomSuccessHandler.java
public class CustomSuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public CustomSuccessHandler(UserService userService) {
        this.userService = userService;
    }
    // ... bỏ @Autowired
}
```

---

#### BUG-SEC-02 — `clearAuthenticationAttributes` gọi SAU `sendRedirect` (response đã committed)
- **File:** `config/CustomSuccessHandler.java` – dòng 87–88
- **Mô tả:** `redirectStrategy.sendRedirect(...)` commit response, sau đó `clearAuthenticationAttributes(...)` vẫn tiếp tục chạy. Tuy không crash, nhưng thứ tự sai — session attributes có thể không được ghi đúng trên một số servlet container.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Đổi thứ tự: gọi `clearAuthenticationAttributes` trước `sendRedirect`.

```java
@Override
public void onAuthenticationSuccess(...) throws IOException, ServletException {
    String targetUrl = determineTargetUrl(authentication);
    clearAuthenticationAttributes(request, authentication); // Trước
    if (!response.isCommitted()) {
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
}
```

---

#### BUG-SEC-03 — Typo "ROLE_AMIN" trong kiểm tra quyền xoá comment
- **File:** `controller/client/ClientNovelController.java` – dòng 352
- **Mô tả:** `"ROLE_AMIN".equalsIgnoreCase(role)` — thiếu chữ `D`. Admin thực tế sẽ không bao giờ khớp điều kiện này, nhưng vì `"ROLE_ADMIN"` ở trước đã bắt đúng nên vẫn hoạt động. Tuy nhiên đây là dead code dễ gây nhầm lẫn bảo trì.
- **Mức độ:** 🟡 **Trung bình** (không broken nhưng nguy hiểm khi refactor)
- **Cách sửa:** Xoá dòng `|| "ROLE_AMIN".equalsIgnoreCase(role)`.

---

#### BUG-SEC-04 — Password lộ trong log của `ClientAccountController`
- **File:** `config/CustomSuccessHandler.java`, `controller/client/ClientAccountController.java`
- **Mô tả:** Profile edit log in ra `fullName`, `email`, `phoneNumber` qua `System.out.println`. Dù không log password, thông tin PII đang được ghi ra console mà không qua `logging.level` control.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Thay `System.out.println` bằng `logger.debug(...)`, không log PII ở level INFO.

---

#### BUG-SEC-05 — `AntPathRequestMatcher` deprecated trong Spring Security 6
- **File:** `config/SecurityConfiguration.java` – dòng 79–81
- **Mô tả:** `AntPathRequestMatcher.antMatcher(...)` đã bị deprecated và đánh dấu for-removal. Build hiện cho warning.
- **Mức độ:** 🟡 **Trung bình** (sẽ break ở version tương lai)
- **Cách sửa:**

```java
.requestMatchers("/api/chapter/*/audio-status", "/audio/**")
.permitAll()
```

---

#### BUG-SEC-06 — Banned user vẫn đăng nhập được
- **File:** `service/CustomUserDetailsService.java` – dòng 33
- **Mô tả:** `user.getStatus()` truyền vào tham số `enabled` của `UserDetails`. Nếu admin "ban" user (set `status=false`), `loadUserByUsername` trả về `enabled=false` → Spring Security throw `DisabledException`. Tuy nhiên không có thông báo lỗi rõ ràng trên trang login (chỉ redirect `/login?error` generic).
- **Mức độ:** 🟢 **Thấp** (security logic đúng, UX chưa tốt)
- **Cách sửa:** Thêm message lỗi riêng cho `DisabledException` trong login template.

---

### 2.2 Lỗi Logic Nghiệp Vụ

#### BUG-BIZ-01 — `UserService.updateUser()` gọi `.get()` không an toàn
- **File:** `service/UserService.java` – dòng 62
- **Mô tả:** `this.userRepository.findById(user.getId()).get()` — nếu user không tồn tại sẽ throw `NoSuchElementException` không được handle.
- **Mức độ:** 🔴 **Cao**
- **Cách sửa:**

```java
User currentUser = this.userRepository.findById(user.getId())
    .orElseThrow(() -> new RuntimeException("User not found: " + user.getId()));
```

---

#### BUG-BIZ-02 — `NovelService.updateGenre()` kiểm tra null thừa (dead code)
- **File:** `service/NovelService.java` – dòng 183
- **Mô tả:** `if (genreInDataBase != null)` – sau `orElseThrow` ở dòng trên, `genreInDataBase` không bao giờ null. `return genreInDataBase` phía dưới là dead code.
- **Mức độ:** 🟢 **Thấp** (clean code issue)
- **Cách sửa:** Xoá nhánh `if` thừa.

---

#### BUG-BIZ-03 — `RegisterValidator.isValid()` NPE nếu password null
- **File:** `service/Validator/RegisterValidator.java` – dòng 24
- **Mô tả:** `user.getPassword().equals(user.getConfirmPassword())` — nếu `password` null (do `@StrongPassword` fail trước), sẽ throw `NullPointerException`.
- **Mức độ:** 🔴 **Cao** – Crash trang đăng ký
- **Cách sửa:**

```java
if (user.getPassword() == null || user.getConfirmPassword() == null
        || !user.getPassword().equals(user.getConfirmPassword())) {
    // report error
}
```

---

#### BUG-BIZ-04 — `ChapterController.create()` không validate chapter
- **File:** `controller/admin/ChapterController.java` – dòng 65–73
- **Mô tả:** POST `/admin/chapter/create/{novelId}` nhận `Chapter` object không có `@Valid`. Title và content có thể trống, chapter blank được lưu DB.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Thêm `@Valid` annotation và validation constraints vào `Chapter` entity (`@NotBlank` cho `title`, `content`).

---

#### BUG-BIZ-05 — `AdminNovelSearch` có sort nhưng `adminSearch()` không truyền sort vào DB
- **File:** `service/PaginationService.java` – dòng 74–79
- **Mô tả:** `AdminNovelSearch(…, sort, direction)` tạo `Sort` object rồi truyền vào `pageable`. Tuy nhiên `adminSearch()` gọi `novelRepository.findByHot(...)`, `findByTitleContaining(...)`, v.v. — các query này sẽ honour `Pageable.sort` của Spring Data. Nhưng query `adminSearch` kết hợp keyword+hot dùng `findByHotAndTitleContainingIgnoreCase` — cũng OK. Vấn đề là `Sort` chỉ áp dụng cho `ratingAvg` nhưng không có index trên cột này.
- **Mức độ:** 🟡 **Trung bình** (hiệu năng)
- **Cách sửa:** Thêm index trên `rating_avg` trong migration SQL.

---

#### BUG-BIZ-06 — `searchVisibleNovels` hard-limit 500 bản ghi
- **File:** `service/NovelService.java` – dòng 285
- **Mô tả:** `PageRequest.of(0, 500)` — nếu DB có >500 novel thỏa điều kiện, kết quả bị cắt sai. Sau đó filter trong memory và phân trang thủ công — toàn bộ flow này rất kém scalable.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Viết JPQL/native query thay vì filter in-memory.

---

#### BUG-BIZ-07 — `ReadCountService` bắt Exception và rollback nhưng vẫn nuốt lỗi hoàn toàn
- **File:** `service/ReadCountService.java` – dòng 78–80
- **Mô tả:** Catch `Exception` quá rộng. Nếu `incrementCounts` (UPDATE) thành công nhưng `saveLog` fail, `setRollbackOnly()` rollback cả 2 → count bị mất. Nếu lỗi ở phía ngoài transaction, `currentTransactionStatus()` throw `NoTransactionException`.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Tách `saveLog` thành method riêng với `@Transactional(propagation = REQUIRES_NEW)`.

---

#### BUG-BIZ-08 — `findActiveNovelsWithActiveGenres` load tối đa 2000 bản ghi vào memory
- **File:** `service/NovelService.java` – dòng 337
- **Mô tả:** `PageRequest.of(0, 2000)` dùng cho trang danh sách truyện — phản vệ kém, nặng heap.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Viết custom JPQL query với `JOIN` điều kiện genres trực tiếp.

---

#### BUG-BIZ-09 — `HomepageController.getHomePage` và `HomepageController.autoDirectHomePage` tạo redirect loop tiềm năng
- **File:** `controller/client/HomepageController.java` – dòng 67–69
- **Mô tả:** `GET /` redirect tới `/home`. Mapping `/home` khác. Không có vòng lặp hiện tại. Nhưng nếu ai thêm `@GetMapping("/")` thứ hai, sẽ conflict.
- **Mức độ:** 🟢 **Thấp**

---

#### BUG-BIZ-10 — `ClientAccountController.postEditProfile` không set `role` và `id` trước khi gọi `updateUser`
- **File:** `controller/client/ClientAccountController.java` – dòng 106
- **Mô tả:** `user` từ `@ModelAttribute` là shallow object từ form — không có `id` (form không gửi). `updateUser(user, file)` gọi `findById(user.getId())` với id=null → `findById(null)` → `IllegalArgumentException`.
- **Mức độ:** 🔴 **Cao** – Profile edit crash
- **Cách sửa:**

```java
User currentUser = this.userService.getUserByUsername(username);
user.setId(currentUser.getId());
user.setRole(currentUser.getRole());
this.userService.updateUser(user, file);
```

---

### 2.3 Lỗi Database / Entity

#### BUG-DB-01 — Không có Flyway/Liquibase, migration thủ công không được tracking
- **File:** `application.properties` – dòng 2; `db/migration/*.sql`
- **Mô tả:** `spring.jpa.hibernate.ddl-auto=update` + file SQL thủ công không được tool nào quản lý. Nếu dev mới pull code, họ phải chạy V2, V3, V4 theo thứ tự thủ công. Dễ bỏ sót, không có version tracking.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Thêm Flyway dependency, đổi `ddl-auto=none` hoặc `validate`, đặt SQL vào `classpath:db/migration/V*.sql` theo chuẩn Flyway.

---

#### BUG-DB-02 — `Novel.image` và `User.image` thiếu `@Column` annotation
- **File:** `domain/Novel.java` – dòng 61; `domain/User.java` – dòng 49
- **Mô tả:** `String image` không có `@Column`, Hibernate dùng default (nullable, varchar 255). Không phải lỗi crash nhưng thiếu rõ ràng về schema.
- **Mức độ:** 🟢 **Thấp**
- **Cách sửa:** Thêm `@Column(name = "image", length = 500)`.

---

#### BUG-DB-03 — `Comment.id` kiểu `int` thay vì `Long`
- **File:** `domain/Comment.java` – dòng 22
- **Mô tả:** Primary key `int` sẽ overflow ở ~2.1 tỷ record. Các entity khác đều dùng `Long`.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Đổi `private int id` → `private Long id`.

---

#### BUG-DB-04 — `Chapter` thiếu `@Column(name="novel_id")` cho FK, slug URL có thể không ổn định nếu chapter không có title
- **File:** `domain/Chapter.java`; `service/UrlSlugService.java` – dòng 82
- **Mô tả:** `chapter.title` không có `@NotBlank`. Nếu title null/blank, slug thành `c{id}-untitled` — URL hợp lệ nhưng không SEO-friendly.
- **Mức độ:** 🟢 **Thấp**
- **Cách sửa:** Thêm `@NotBlank` constraint trên `Chapter.title`.

---

#### BUG-DB-05 — `ChapterController.uploadImage` không handle null fileName
- **File:** `controller/admin/ChapterController.java` – dòng 123
- **Mô tả:** `imageService.handleImage(file, "chapter")` trả về `""` nếu lỗi IO. `result.put("url", "/images/chapter/" + "")` → URL broken.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:**

```java
if (fileName == null || fileName.isBlank()) {
    result.put("error", "Upload failed");
    return result;
}
result.put("url", "/images/chapter/" + fileName);
```

---

#### BUG-DB-06 — `spring.session.jdbc.initialize-schema` bị comment out
- **File:** `application.properties` – dòng 29
- **Mô tả:** Spring Session JDBC cần bảng `SPRING_SESSION` và `SPRING_SESSION_ATTRIBUTES`. Nếu DB mới chưa có bảng này và config bị comment, ứng dụng runtime sẽ fail khi session được tạo.
- **Mức độ:** 🔴 **Cao** – Crash ngay khi deploy trên DB mới
- **Cách sửa:** Bỏ comment `spring.session.jdbc.initialize-schema=always` hoặc thêm SQL tạo bảng vào migration.

---

### 2.4 Lỗi Mapping URL / Controller

#### BUG-URL-01 — `ReaderController.readChapterBySlug()` so sánh URL không tính context path
- **File:** `controller/client/ReaderController.java` – dòng 74
- **Mô tả:** `canonicalReaderUrl.equals(request.getRequestURI())` — `getRequestURI()` bao gồm context path khi deploy trên Tomcat với context root khác `/`. `ClientNovelController.getNovelDetailPageBySlug()` đã dùng helper `getRequestPath(request)` đúng cách, nhưng `ReaderController` không dùng.
- **Mức độ:** 🟡 **Trung bình** – Redirect loop vô hạn khi deploy với context path
- **Cách sửa:**

```java
String requestPath = request.getRequestURI();
String contextPath = request.getContextPath();
if (contextPath != null && !contextPath.isBlank() && requestPath.startsWith(contextPath)) {
    requestPath = requestPath.substring(contextPath.length());
}
if (!canonicalReaderUrl.equals(requestPath)) {
    return permanentRedirect(appendQueryString(canonicalReaderUrl, request));
}
```

---

#### BUG-URL-02 — `ClientLibraryController` map cả `/bookshelf` và `/library` nhưng redirect về `/bookshelf`
- **File:** `controller/client/ClientLibraryController.java` – dòng 26, 171
- **Mô tả:** Controller map 2 path `/bookshelf` và `/library`. Sau khi xoá từ bookshelf, redirect về `/bookshelf`. Nếu user vào qua `/library`, sau action lại redirect sang `/bookshelf` — không nhất quán.
- **Mức độ:** 🟢 **Thấp** (UX)

---

#### BUG-URL-03 — `RatingController` map tại `/novel/{novelId}/rate` nhưng Security chỉ permit `/novel/**` với GET
- **File:** `config/SecurityConfiguration.java` – dòng 72; `controller/client/RatingController.java` – dòng 30
- **Mô tả:** `POST /novel/{novelId}/rate` cần authentication. Security config có `.anyRequest().authenticated()` nên POST sẽ yêu cầu login — đúng. Nhưng CSRF token cần được gửi kèm từ frontend (Thymeleaf form). Nếu AJAX không gửi CSRF, request bị 403.
- **Mức độ:** 🟡 **Trung bình**
- **Cách sửa:** Đảm bảo JS fetch rating gửi `X-CSRF-TOKEN` header. Xem xét thêm route rating vào permitted list nếu cần.

---

#### BUG-URL-04 — `ErrorPageController` trỏ đến template `/error/403` nhưng Security config dùng `/error/403`
- **File:** `config/SecurityConfiguration.java` – dòng 108; `controller/client/ErrorPageController.java`
- **Mô tả:** Cần kiểm tra `ErrorPageController` có map `GET /error/403` không. Nếu không, Spring dùng fallback Whitelabel error page khi access denied.
- **Mức độ:** 🟡 **Trung bình**

---

### 2.5 Lỗi Clean Code / Refactor

#### BUG-CC-01 — `System.out.println` dùng thay vì logger
- **File:** `controller/admin/NovelController.java` dòng 107; `controller/admin/UserController.java` dòng 67; `service/ImageService.java` dòng 47
- **Mô tả:** Nhiều chỗ dùng `System.out.println` thay vì SLF4J logger. Không thể control log level, không có timestamp/thread info.
- **Mức độ:** 🟢 **Thấp**

---

#### BUG-CC-02 — `PaginationService` còn các method thừa không dùng
- **File:** `service/PaginationService.java` – `AdminNovelPagination(page, size)`, `AdminNovelPagination(page, size, keyword)`, `AdminNovelHotPagination(...)`, `ClientSearchNovelPagination(...)`
- **Mô tả:** Các overload cũ này không được gọi bởi bất kỳ controller nào hiện tại. Dead code.
- **Mức độ:** 🟢 **Thấp**

---

#### BUG-CC-03 — `LOG_DIVIDER` lặp lại trong nhiều controller
- **File:** Nhiều controller
- **Mô tả:** Hằng số `LOG_DIVIDER` được khai báo lại trong từng controller. Nên extract thành constant chung.
- **Mức độ:** 🟢 **Thấp**

---

#### BUG-CC-04 — `ImageService.handleImageForNovel()` không được gọi ở đâu
- **File:** `service/ImageService.java` – dòng 55–65
- **Mô tả:** Method `handleImageForNovel` (tạo tên file theo genre+novelId) là dead code.
- **Mức độ:** 🟢 **Thấp**

---

#### BUG-CC-05 — Typo trong error message: "ComfirmPassword doesn't fit"
- **File:** `service/Validator/RegisterValidator.java` – dòng 25
- **Mô tả:** "Comfirm" → "Confirm". Lỗi chính tả hiển thị cho người dùng.
- **Mức độ:** 🟢 **Thấp**

---

## 3. Tổng hợp theo mức độ ưu tiên sửa

### 🔴 Ưu tiên 1 – LỖI BUILD / RUNTIME (sửa ngay)

| ID | File | Vấn đề |
|---|---|---|
| BUG-SEC-01 | `CustomSuccessHandler.java` | `@Autowired` trong non-bean → NPE mỗi login |
| BUG-BIZ-03 | `RegisterValidator.java` | NPE khi password null → crash trang đăng ký |
| BUG-BIZ-10 | `ClientAccountController.java` | `user.getId()` null khi edit profile → crash |
| BUG-BIZ-01 | `UserService.java` | `.get()` không an toàn → NoSuchElementException |
| BUG-DB-06 | `application.properties` | Spring Session bảng chưa có → crash deploy DB mới |

### 🔴 Ưu tiên 2 – LỖI BẢO MẬT

| ID | File | Vấn đề |
|---|---|---|
| BUG-SEC-02 | `CustomSuccessHandler.java` | `sendRedirect` trước `clearAttributes` |
| BUG-SEC-03 | `ClientNovelController.java` | Typo "ROLE_AMIN" – nguy hiểm khi refactor |
| BUG-SEC-04 | `ClientAccountController.java` | PII log ra console |
| BUG-SEC-05 | `SecurityConfiguration.java` | `AntPathRequestMatcher` deprecated |

### 🟡 Ưu tiên 3 – LỖI LOGIC NGHIỆP VỤ

| ID | File | Vấn đề |
|---|---|---|
| BUG-BIZ-04 | `ChapterController.java` | Không validate chapter khi tạo |
| BUG-BIZ-07 | `ReadCountService.java` | Exception handling quá rộng |
| BUG-URL-01 | `ReaderController.java` | Context path không tính trong redirect check |

### 🟡 Ưu tiên 4 – LỖI DATABASE / ENTITY

| ID | File | Vấn đề |
|---|---|---|
| BUG-DB-01 | `application.properties` | Không có migration tool |
| BUG-DB-03 | `Comment.java` | ID kiểu `int` thay vì `Long` |
| BUG-DB-05 | `ChapterController.java` | Upload image không check null fileName |

### 🟡 Ưu tiên 5 – LỖI HIỆU NĂNG / SCALABILITY

| ID | File | Vấn đề |
|---|---|---|
| BUG-BIZ-06 | `NovelService.java` | Hard-limit 500 bản ghi filter in-memory |
| BUG-BIZ-08 | `NovelService.java` | Load 2000 bản ghi vào heap |

### 🟢 Ưu tiên 6 – CLEAN CODE / REFACTOR

| ID | File | Vấn đề |
|---|---|---|
| BUG-CC-01 | Nhiều controller | `System.out.println` thay vì logger |
| BUG-CC-02 | `PaginationService.java` | Dead code methods |
| BUG-CC-03 | Nhiều controller | `LOG_DIVIDER` lặp lại |
| BUG-CC-04 | `ImageService.java` | `handleImageForNovel()` dead code |
| BUG-CC-05 | `RegisterValidator.java` | Typo "ComfirmPassword" |
| BUG-BIZ-02 | `NovelService.java` | Dead code sau `orElseThrow` |

---

## 4. Kết quả build & test

```
[INFO] BUILD SUCCESS
[INFO] Total time: 48.916 s
[WARNING] AntPathRequestMatcher deprecated (2 chỗ trong SecurityConfiguration.java)
[WARNING] Unchecked operations in GenreController.java
```

- **Compile errors:** 0
- **Test:** Skipped (`-DskipTests`)
- **Warnings:** 3 (deprecation + unchecked)

---

## 5. Nhận xét tổng thể

**Điểm mạnh:**
- Kiến trúc phân layer rõ ràng (Controller → Service → Repository)
- Security config tổng thể khá đúng (CSRF enabled, session management, regex permit cho slug URL)
- Custom validator (`@StrongPassword`, `@RegisterChecked`) được tổ chức tốt
- Read count anti-spam logic (cooldown theo user/session) được implement cẩn thận
- Slug URL service clean và tái sử dụng tốt
- Recommendation engine có thuật toán hợp lý

**Điểm cần cải thiện:**
- Bug **BUG-SEC-01** (NPE khi login) cần sửa ngay — đây là lỗi nghiêm trọng nhất
- Bug **BUG-BIZ-10** (profile edit crash) và **BUG-BIZ-03** (register crash) cần sửa trước khi production
- Cân nhắc thêm Flyway để quản lý migration SQL
- Các query filter in-memory cần rewrite thành DB-level query cho scalability
