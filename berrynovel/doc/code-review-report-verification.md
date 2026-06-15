# Báo cáo đối chiếu `code-review-report.md`

**Ngày kiểm chứng:** 2026-06-15  
**Phạm vi:** đối chiếu `doc/code-review-report.md` với toàn bộ `src/main`, `src/test`, `pom.xml` và cấu hình resources.  
**Kết quả kiểm thử:** `.\mvnw.cmd test` chạy thành công, 13 tests pass. Lưu ý test hiện tại có kết nối MySQL local và không chứng minh được kịch bản deploy DB mới.

## 1. Kết luận nhanh

Báo cáo gốc **không sai hoàn toàn**, nhưng có một số claim bị đánh giá quá nặng hoặc sai với code hiện tại. Các lỗi nên ưu tiên thật sự là:

| Mức ưu tiên | Vấn đề | Kết luận |
|---|---|---|
| Cao | Validator đăng ký có thể NPE khi password null | Đúng, còn nặng hơn report vì cả `StrongPasswordValidator` cũng không null-safe |
| Cao | Spring Session JDBC chưa có schema migration rõ ràng | Đúng trong môi trường DB mới |
| Cao | `ReaderController` có nguy cơ redirect loop khi deploy dưới context path | Đúng |
| Trung bình - Cao | Search/list truyện dùng hard-limit 500/2000 rồi phân trang in-memory | Đúng, ảnh hưởng dữ liệu và performance khi dữ liệu lớn |
| Trung bình | `UserService.updateUser()` dùng `.get()` và tin hidden field `role.name` từ form | Đúng một phần; profile form có gửi `id/role`, nhưng service vẫn dễ lỗi và có rủi ro phân quyền dữ liệu |
| Trung bình | Log PII/debug/security quá nhiều | Đúng, nên chỉnh trước production |

Các claim quan trọng trong report gốc nhưng **không đúng hoặc cần hạ mức**:

| ID gốc | Kết luận |
|---|---|
| `BUG-SEC-01` | Sai: `CustomSuccessHandler` được trả về từ `@Bean`, nên vẫn là Spring bean; `@Autowired` không mặc nhiên null |
| `BUG-BIZ-10` | Sai với code hiện tại: form profile có hidden `id` và `role.name`; không phải chắc chắn crash |
| `BUG-DB-04` | Sai phần FK: `Chapter` đã có `@JoinColumn(name = "novel_id")` |
| `BUG-URL-03` | Sai phần security: `/novel/**` đang `permitAll` cho mọi method; JS đã gửi CSRF token |
| `BUG-URL-04` | Sai: có `ErrorPageController` mapping `/error/403` và template tồn tại |
| `BUG-BIZ-09` | Không phải lỗi hiện tại: `/` redirect `/home`, `/home` render page, không có loop |

## 2. Đối chiếu từng lỗi trong report gốc

### Security

| ID | Kết luận kiểm chứng | Mức ảnh hưởng thật | Ghi chú |
|---|---|---|---|
| `BUG-SEC-01` | **Sai** | Không phải bug runtime | `SecurityConfiguration.customSuccessHandler()` là `@Bean` tại `src/main/java/com/bn/berrynovel/config/SecurityConfiguration.java:37`; object `new CustomSuccessHandler()` vẫn được Spring quản lý. Nên đổi sang constructor injection cho sạch, nhưng không phải NPE login chắc chắn. |
| `BUG-SEC-02` | **Đúng một phần** | Thấp - Trung bình | `sendRedirect` trước `clearAuthenticationAttributes` tại `CustomSuccessHandler.java:87-88`. Nên đảo thứ tự để rõ lifecycle, nhưng session server-side thường vẫn set được; không nên xếp cao. |
| `BUG-SEC-03` | **Đúng** | Thấp | Có typo `"ROLE_AMIN"` tại `ClientNovelController.java:352`. Vì `"ROLE_ADMIN"` đã check ngay trước đó nên hiện không làm admin mất quyền. |
| `BUG-SEC-04` | **Đúng, mô tả gốc hơi lệch** | Trung bình | Không thấy log password, nhưng có log username/email/phone/fullName ở `ClientAccountController` và nhiều `System.out.println`. Đây là PII/logging risk. |
| `BUG-SEC-05` | **Đúng** | Thấp - Trung bình | `AntPathRequestMatcher` deprecated tại `SecurityConfiguration.java:79-81`. Đây là rủi ro nâng version, chưa phải lỗi production hiện tại. |
| `BUG-SEC-06` | **Đúng một phần** | Thấp | Banned user không đăng nhập được vì `enabled=user.getStatus()` tại `CustomUserDetailsService.java:33`. Vấn đề còn lại là UX login chỉ có `/login?error` generic tại `SecurityConfiguration.java:103`. |

### Logic nghiệp vụ

| ID | Kết luận kiểm chứng | Mức ảnh hưởng thật | Ghi chú |
|---|---|---|---|
| `BUG-BIZ-01` | **Đúng** | Trung bình | `UserService.updateUser()` dùng `findById(...).get()` tại `UserService.java:62`; nếu id không tồn tại sẽ throw `NoSuchElementException`. Nên dùng `orElseThrow` có message. |
| `BUG-BIZ-02` | **Đúng** | Thấp | `updateGenre()` đã `orElseThrow` rồi vẫn check null tại `NovelService.java:181-183`. Clean code/dead code. |
| `BUG-BIZ-03` | **Đúng, còn thiếu một điểm** | Cao | `RegisterValidator.java:24` có NPE nếu password null. Ngoài ra `StrongPasswordValidator.java:11` cũng gọi `value.matches(...)` nên cũng NPE khi password null. |
| `BUG-BIZ-04` | **Đúng** | Trung bình | `ChapterController.create()` không dùng `@Valid` tại `ChapterController.java:65`; `Chapter.title/content` không có `@NotBlank` tại `Chapter.java:22/29`. UI có `required` cho title, nhưng backend vẫn bypass được. |
| `BUG-BIZ-05` | **Đúng một phần** | Thấp - Trung bình | Sort rating có truyền qua `Pageable`, nên không sai logic. Tuy nhiên V4 chỉ có index cho `novel_rating.novel_id`, chưa có index cho `Novels.rating_avg/rating_count`; performance có thể kém khi dữ liệu lớn. |
| `BUG-BIZ-06` | **Đúng** | Trung bình - Cao | `searchVisibleNovels()` hard-limit `PageRequest.of(0, 500)` tại `NovelService.java:285`, sau đó filter/sort/paginate in-memory. Có thể mất kết quả đúng khi >500 truyện match. |
| `BUG-BIZ-07` | **Đúng một phần** | Trung bình | `ReadCountService.recordChapterRead()` catch `Exception` và rollback tại `ReadCountService.java:78-79`. Vì method có `@Transactional`, `currentTransactionStatus()` thường hợp lệ. Nhưng nuốt lỗi và rollback cả increment nếu save log fail là tradeoff cần quyết định lại. |
| `BUG-BIZ-08` | **Đúng** | Trung bình - Cao | `findActiveNovelsWithActiveGenres()` hard-limit `PageRequest.of(0, 2000)` tại `NovelService.java:337`, rồi phân trang thủ công. Sai dữ liệu khi vượt 2000, tốn heap/DB. |
| `BUG-BIZ-09` | **Sai / giả định tương lai** | Không đáng tính lỗi | `/` redirect `/home` tại `HomepageController.java:69`; `/home` render view. Không có redirect loop hiện tại. |
| `BUG-BIZ-10` | **Sai với code hiện tại, nhưng có rủi ro thiết kế** | Thấp - Trung bình | Template profile có hidden `id` và `role.name` tại `templates/client/profile/edit.html:41-42`, nên không chắc chắn crash. Tuy nhiên client có thể sửa hidden `role.name`; service không nên tin role từ form profile. |

### Database / Entity

| ID | Kết luận kiểm chứng | Mức ảnh hưởng thật | Ghi chú |
|---|---|---|---|
| `BUG-DB-01` | **Đúng** | Trung bình - Cao | `spring.jpa.hibernate.ddl-auto=update` tại `application.properties:2`; có SQL migration thủ công nhưng `pom.xml` không có Flyway/Liquibase. DB mới dễ thiếu bước hoặc sai thứ tự. |
| `BUG-DB-02` | **Đúng một phần** | Thấp | `Novel.image` không có `@Column` tại `Novel.java:61`; `User.image` có `@Column` tại `User.java:48-49`. Đây chủ yếu là schema clarity. |
| `BUG-DB-03` | **Đúng** | Thấp - Trung bình | `Comment.id` là `int` tại `Comment.java:22`, repository dùng `Integer`. Nên thống nhất `Long`, nhưng overflow chỉ xảy ra khi dữ liệu rất lớn. |
| `BUG-DB-04` | **Sai một phần** | Thấp | FK `Chapter.novel` đã có `@JoinColumn(name = "novel_id")` tại `Chapter.java:37`. Phần thiếu validation title/content thì đúng và đã nằm ở `BUG-BIZ-04`. |
| `BUG-DB-05` | **Đúng** | Trung bình | `uploadImage()` luôn trả `"/images/chapter/" + fileName`; `ImageService.saveImage()` có thể trả `""` khi lỗi tại `ImageService.java:72/80`. Cần trả error JSON thay vì URL rỗng. |
| `BUG-DB-06` | **Đúng** | Cao khi deploy DB mới | `spring.session.store-type=jdbc` bật tại `application.properties:27`, nhưng `spring.session.jdbc.initialize-schema` bị comment tại dòng 29. Nếu DB chưa có `SPRING_SESSION`, session runtime có thể lỗi. |

### URL / Controller

| ID | Kết luận kiểm chứng | Mức ảnh hưởng thật | Ghi chú |
|---|---|---|---|
| `BUG-URL-01` | **Đúng** | Cao nếu deploy dưới context path | `ReaderController` so sánh canonical URL với `request.getRequestURI()` tại `ReaderController.java:74`. `ClientNovelController` đã có helper bỏ context path tại `ClientNovelController.java:391-395`; nên áp dụng tương tự. |
| `BUG-URL-02` | **Đúng** | Thấp | Controller map cả `/bookshelf` và `/library`, nhưng delete redirect `/bookshelf` tại `ClientLibraryController.java:171`. Đây là UX consistency. |
| `BUG-URL-03` | **Sai phần chính** | Thấp | Security đang `permitAll("/novel/**")` tại `SecurityConfiguration.java:72`, không phải chỉ GET. Controller tự check login. CSRF token đã được render ở `novel/show.html:95` và JS gửi header tại `star-rating.js:47`. |
| `BUG-URL-04` | **Sai** | Không phải lỗi | Có `@GetMapping("/error/403")` tại `ErrorPageController.java:9` và template `templates/error/403.html` tồn tại. |

### Clean code / refactor

| ID | Kết luận kiểm chứng | Mức ảnh hưởng thật | Ghi chú |
|---|---|---|---|
| `BUG-CC-01` | **Đúng** | Thấp - Trung bình | Có `System.out.println` trong app main/controller/service; `ImageService` còn `printStackTrace`. Nên dùng logger và bỏ log startup bean names. |
| `BUG-CC-02` | **Có vẻ đúng** | Thấp | Một số overload trong `PaginationService` không thấy caller trong `src/main`. Xóa cần kiểm tra template/đường dùng tương lai. |
| `BUG-CC-03` | **Đúng** | Thấp | `LOG_DIVIDER` lặp ở nhiều controller. Không ảnh hưởng runtime. |
| `BUG-CC-04` | **Đúng** | Thấp | `ImageService.handleImageForNovel()` không thấy caller. |
| `BUG-CC-05` | **Đúng** | Thấp | Typo `"ComfirmPassword doesn't fit"` tại `RegisterValidator.java:25`. |

## 3. Phát hiện bổ sung ngoài report gốc

| ID mới | Mức ảnh hưởng | Vấn đề |
|---|---|---|
| `ADD-01` | Cao | `StrongPasswordValidator.isValid()` không null-safe (`value.matches(...)`). Đây là lỗi cùng nhóm với `BUG-BIZ-03`, nên sửa chung. |
| `ADD-02` | Trung bình - Cao | `application.properties` hard-code `spring.datasource.username=root` và `spring.datasource.password=123456`. Nếu file này dùng cho production thì là security/config risk. |
| `ADD-03` | Trung bình | `logging.level.org.springframework.security=DEBUG` và `logging.pattern.console=%msg%n` đang để trong config mặc định. Production sẽ lộ nhiều thông tin security/session hơn cần thiết. |
| `ADD-04` | Thấp - Trung bình | `BerrynovelApplication.main()` in toàn bộ bean name khi startup (`BerrynovelApplication.java:19`). Nên bỏ hoặc chuyển debug logger. |
| `ADD-05` | Trung bình | `UserService.updateUser()` dùng `role.name` từ form cho cả admin update và client profile update. Với profile, role nên lấy từ DB/current user, không lấy từ hidden input. |

## 4. Ưu tiên sửa đề xuất

1. Sửa validator đăng ký: null-safe cho `StrongPasswordValidator` và `RegisterValidator`, thêm message đúng chính tả.
2. Sửa cấu hình DB/session: thêm Flyway/Liquibase hoặc ít nhất migration tạo `SPRING_SESSION`; không để `ddl-auto=update` cho production.
3. Sửa `ReaderController` dùng helper bỏ context path giống `ClientNovelController`.
4. Viết lại `searchVisibleNovels()` và `findActiveNovelsWithActiveGenres()` thành query DB-level có pagination thật.
5. Làm cứng `UserService.updateUser()`: dùng `orElseThrow`, không tin `role.name` từ profile form, tách method update profile và admin update.
6. Thêm backend validation cho chapter create/update và xử lý upload image lỗi bằng response JSON lỗi.
7. Dọn logging: bỏ `System.out.println`, bỏ security DEBUG mặc định, không log PII ở INFO.

## 5. Đánh giá lại report gốc

Report gốc hữu ích để chỉ ra nhiều khu vực rủi ro, nhưng cần chỉnh lại mức độ:

- Không nên coi `BUG-SEC-01` là lỗi login NPE nghiêm trọng vì handler là bean qua `@Bean`.
- Không nên coi `BUG-BIZ-10` là profile edit chắc chắn crash vì template hiện có hidden `id/role`.
- Một số mục là clean code hoặc giả định tương lai, không nên đặt ngang hàng với lỗi runtime.
- Các lỗi thật sự nặng nhất nằm ở validation null, DB/session migration, context path redirect, và phân trang/filter in-memory.
