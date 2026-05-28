# Chức năng Đánh giá sao — Kịch bản & Spec

> **Project:** berrynovel (Spring Boot 3.5 + Thymeleaf + MySQL)
> **Date:** 2026-05-28

---

## 1. Tổng quan tính năng

User có thể đánh giá truyện từ **1–5 sao** trên trang detail novel (`/novel/{id}`).  
Mỗi user chỉ đánh giá **một lần duy nhất** per novel, nhưng có thể **thay đổi** điểm.  
Trang hiển thị **điểm trung bình** và **tổng số lượt đánh giá**.

---

## 2. Kịch bản người dùng (User Stories)

### US-1: User chưa đăng nhập xem trang novel

```
Given: User truy cập trang /novel/5 mà không đăng nhập
When:  Trang tải xong
Then:  - Hiển thị điểm trung bình: ★★★★☆ 4.2 (128 ratings)
       - Không hiện form đánh giá
       - Hiện link "Đăng nhập để đánh giá"
```

---

### US-2: User đăng nhập, chưa từng đánh giá novel này

```
Given: User đã đăng nhập, chưa đánh giá novel này
When:  Trang detail novel tải
Then:  - Hiển thị 5 ngôi sao trống (chưa chọn)
       - Hover vào sao nào thì sao đó và các sao trước highlight vàng
       - Click vào sao → submit đánh giá
       - Sau khi submit: hiện "Cảm ơn bạn đã đánh giá!" + cập nhật điểm TB
```

---

### US-3: User đã từng đánh giá novel này

```
Given: User đã đánh giá novel này 4 sao trước đó
When:  Trang detail novel tải
Then:  - Hiển thị 4 sao vàng (điểm cũ của user)
       - User có thể click sao khác để thay đổi điểm
       - Sau khi click: cập nhật điểm TB ngay, không reload trang
```

---

### US-4: Admin xem thống kê đánh giá trong trang quản lý novel

```
Given: Admin truy cập /admin/novel
When:  Xem danh sách novel
Then:  - Mỗi novel có cột "Rating": 4.2 ★ (128)
       - Có thể sort theo rating
```

---

## 3. Quy tắc nghiệp vụ

| Rule | Chi tiết |
|---|---|
| **Một user, một novel, một vote** | Upsert — nếu đã vote thì update, không insert thêm |
| **Chỉ user đăng nhập mới được vote** | Anonymous user chỉ xem |
| **Điểm hợp lệ** | 1, 2, 3, 4, 5 — từ chối giá trị ngoài khoảng |
| **Điểm TB** | Tính từ DB, làm tròn 1 chữ số thập phân |
| **Không cần đọc mới được vote** | Vote ngay khi vào trang detail |
| **Không hiện tên user trong vote** | Chỉ hiện aggregate (TB + count) |

---

## 4. Database Schema

### Bảng mới: `novel_rating`

```sql
CREATE TABLE novel_rating (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id    BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    score       TINYINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_novel_user (novel_id, user_id),
    INDEX idx_novel_rating_novel (novel_id),
    CONSTRAINT fk_nr_novel FOREIGN KEY (novel_id) REFERENCES Novels(id) ON DELETE CASCADE,
    CONSTRAINT fk_nr_user  FOREIGN KEY (user_id)  REFERENCES Users(id)  ON DELETE CASCADE
);
```

### Denormalized cache trên bảng `Novels` (tùy chọn)

Để tránh `AVG()` mỗi lần load trang, có thể thêm 2 cột vào `Novels`:

```sql
ALTER TABLE Novels
    ADD COLUMN rating_avg   DECIMAL(3,1) DEFAULT 0.0,
    ADD COLUMN rating_count INT          DEFAULT 0;
```

> **Cách tiếp cận:** Update 2 cột này mỗi khi có vote mới → query trang detail chỉ cần `SELECT` một bản ghi, không cần `AVG()` JOIN.

---

## 5. Các file cần tạo / chỉnh sửa

### Backend — Java

| File | Action | Mô tả |
|---|---|---|
| `domain/NovelRating.java` | **CREATE** | Entity mapping bảng `novel_rating` |
| `repository/NovelRatingRepository.java` | **CREATE** | JPA repository |
| `service/RatingService.java` | **CREATE** | Business logic: upsert, tính TB |
| `controller/client/RatingController.java` | **CREATE** | `POST /novel/{id}/rate` |
| `domain/Novel.java` | **MODIFY** | Thêm `ratingAvg`, `ratingCount` |
| `controller/client/NovelController.java` | **MODIFY** | Truyền `userRating` vào model |

### Frontend

| File | Action | Mô tả |
|---|---|---|
| `templates/client/novel/show.html` | **MODIFY** | Thêm star widget + hiện điểm TB |
| `static/client/css/novel_detail.css` | **MODIFY** | CSS cho star rating |
| `static/client/js/star-rating.js` | **CREATE** | Fetch POST + hover effect |

---

## 6. Entity — `NovelRating.java`

```java
@Entity
@Table(name = "novel_rating",
    uniqueConstraints = @UniqueConstraint(columnNames = {"novel_id", "user_id"}))
public class NovelRating {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int score; // 1–5

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // getters + setters
}
```

---

## 7. Repository — `NovelRatingRepository.java`

```java
@Repository
public interface NovelRatingRepository extends JpaRepository<NovelRating, Long> {

    Optional<NovelRating> findByNovelIdAndUserId(Long novelId, Long userId);

    @Query("SELECT AVG(r.score) FROM NovelRating r WHERE r.novelId = :novelId")
    Double findAverageScoreByNovelId(@Param("novelId") Long novelId);

    @Query("SELECT COUNT(r) FROM NovelRating r WHERE r.novelId = :novelId")
    long countByNovelId(@Param("novelId") Long novelId);
}
```

---

## 8. Service — `RatingService.java`

```java
@Service
@Transactional
public class RatingService {

    private final NovelRatingRepository ratingRepo;
    private final NovelRepository novelRepo;

    // Upsert: tạo mới hoặc cập nhật vote
    public void rate(Long novelId, Long userId, int score) {
        if (score < 1 || score > 5) throw new IllegalArgumentException("Score must be 1-5");

        NovelRating rating = ratingRepo
            .findByNovelIdAndUserId(novelId, userId)
            .orElseGet(() -> {
                NovelRating r = new NovelRating();
                r.setNovelId(novelId);
                r.setUserId(userId);
                r.setCreatedAt(LocalDateTime.now());
                return r;
            });

        rating.setScore(score);
        rating.setUpdatedAt(LocalDateTime.now());
        ratingRepo.save(rating);

        // Cập nhật cache trên Novel
        refreshNovelRatingCache(novelId);
    }

    public Optional<Integer> getUserRating(Long novelId, Long userId) {
        return ratingRepo.findByNovelIdAndUserId(novelId, userId)
            .map(NovelRating::getScore);
    }

    private void refreshNovelRatingCache(Long novelId) {
        Double avg   = ratingRepo.findAverageScoreByNovelId(novelId);
        long   count = ratingRepo.countByNovelId(novelId);

        novelRepo.findById(novelId).ifPresent(novel -> {
            novel.setRatingAvg(avg != null
                ? Math.round(avg * 10.0) / 10.0  // 1 chữ số thập phân
                : 0.0);
            novel.setRatingCount((int) count);
            novelRepo.save(novel);
        });
    }
}
```

---

## 9. Controller — `RatingController.java`

```java
@Controller
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/novel/{novelId}/rate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rate(
            @PathVariable Long novelId,
            @RequestParam int score,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Login required"));
        }

        Long userId = getCurrentUserId(auth); // lấy từ UserDetails

        try {
            ratingService.rate(novelId, userId, score);
            // Trả về điểm TB mới để cập nhật UI không cần reload
            Novel novel = novelRepo.findById(novelId).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "ratingAvg",   novel.getRatingAvg(),
                "ratingCount", novel.getRatingCount(),
                "userScore",   score
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
```

---

## 10. HTML — Thêm vào `show.html`

Chèn vào trong `.info-card`, ngay dưới phần `Reads:`:

```html
<!-- ── STAR RATING ─────────────────────────────────── -->
<div class="rating-section mt-2" id="rating-section">

    <!-- Hiển thị điểm TB (luôn visible) -->
    <div class="rating-display">
        <span class="rating-stars-display" id="rating-stars-display"
              th:attr="data-avg=${novel.ratingAvg ?: 0}">
        </span>
        <span class="rating-text">
            <strong id="rating-avg"
                th:text="${novel.ratingAvg != null and novel.ratingAvg > 0}
                         ? ${#numbers.formatDecimal(novel.ratingAvg, 1, 1)}
                         : '—'">—</strong>
            <span class="text-muted" id="rating-count"
                  th:text="'(' + (${novel.ratingCount ?: 0}) + ' ratings)'">
            </span>
        </span>
    </div>

    <!-- Form đánh giá (chỉ khi đăng nhập) -->
    <div sec:authorize="isAuthenticated()" class="rating-input mt-2">
        <span class="rating-label">Your rating:</span>
        <div class="star-widget"
             th:attr="data-novel-id=${novel.id},
                      data-user-score=${userRating ?: 0},
                      data-csrf=${_csrf.token}">
            <span class="star" data-value="1">★</span>
            <span class="star" data-value="2">★</span>
            <span class="star" data-value="3">★</span>
            <span class="star" data-value="4">★</span>
            <span class="star" data-value="5">★</span>
        </div>
        <span class="rating-feedback" id="rating-feedback"></span>
    </div>

    <!-- Link login (khi chưa đăng nhập) -->
    <div sec:authorize="!isAuthenticated()" class="mt-2">
        <a th:href="@{/login}" class="text-muted small">Log in to rate this novel</a>
    </div>
</div>
<!-- ── END STAR RATING ────────────────────────────── -->
```

---

## 11. JavaScript — `star-rating.js`

```javascript
(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const widget = document.querySelector(".star-widget");
        if (!widget) return;

        const stars      = widget.querySelectorAll(".star");
        const novelId    = widget.dataset.novelId;
        const csrf       = widget.dataset.csrf;
        const userScore  = parseInt(widget.dataset.userScore, 10) || 0;
        const feedback   = document.getElementById("rating-feedback");

        // Highlight sao theo userScore hiện tại
        highlightStars(userScore);

        // Hover effect
        stars.forEach(star => {
            star.addEventListener("mouseenter", () =>
                highlightStars(parseInt(star.dataset.value, 10)));
            star.addEventListener("mouseleave", () =>
                highlightStars(parseInt(widget.dataset.userScore, 10) || 0));
            star.addEventListener("click", () =>
                submitRating(parseInt(star.dataset.value, 10)));
        });

        function highlightStars(upTo) {
            stars.forEach(s => {
                const v = parseInt(s.dataset.value, 10);
                s.classList.toggle("active", v <= upTo);
            });
        }

        function submitRating(score) {
            fetch(`/novel/${novelId}/rate`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-CSRF-TOKEN": csrf
                },
                body: `score=${score}`
            })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    // Cập nhật data-user-score để hover/leave hoạt động đúng
                    widget.dataset.userScore = score;
                    highlightStars(score);

                    // Cập nhật điểm TB trên UI
                    const avgEl   = document.getElementById("rating-avg");
                    const countEl = document.getElementById("rating-count");
                    if (avgEl)   avgEl.textContent   = data.ratingAvg.toFixed(1);
                    if (countEl) countEl.textContent = `(${data.ratingCount} ratings)`;

                    // Cập nhật visual sao tổng hợp
                    renderAverageStars(data.ratingAvg);

                    feedback.textContent = "Thanks for rating!";
                    setTimeout(() => { feedback.textContent = ""; }, 3000);
                }
            })
            .catch(() => {
                feedback.textContent = "Error. Please try again.";
            });
        }

        // Render sao TB (filled / half / empty)
        function renderAverageStars(avg) {
            const el = document.getElementById("rating-stars-display");
            if (!el) return;
            let html = "";
            for (let i = 1; i <= 5; i++) {
                if (avg >= i)          html += '<span class="star-filled">★</span>';
                else if (avg >= i - 0.5) html += '<span class="star-half">★</span>';
                else                   html += '<span class="star-empty">☆</span>';
            }
            el.innerHTML = html;
        }

        // Render sao TB lúc tải trang
        const initAvg = parseFloat(
            document.getElementById("rating-stars-display")?.dataset.avg || "0"
        );
        renderAverageStars(initAvg);
    });
})();
```

---

## 12. CSS — thêm vào `novel_detail.css`

```css
/* ── Star Rating ─────────────────────────────────── */
.rating-section { line-height: 1.6; }

.rating-display {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.rating-text { font-size: 0.9rem; color: #555; }

/* Sao tổng hợp (display only) */
.star-filled { color: #f5a623; }
.star-half   { color: #f5a623; opacity: 0.5; }
.star-empty  { color: #ccc; }

/* Widget đánh giá */
.rating-input { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.rating-label { font-size: 0.85rem; color: #666; }

.star-widget  { display: inline-flex; gap: 4px; cursor: pointer; }
.star-widget .star {
    font-size: 1.6rem;
    color: #ddd;
    transition: color 0.15s, transform 0.1s;
    user-select: none;
}
.star-widget .star:hover,
.star-widget .star.active {
    color: #f5a623;
    transform: scale(1.15);
}

.rating-feedback {
    font-size: 0.82rem;
    color: #28a745;
    font-style: italic;
    min-height: 1.2em;
}
```

---

## 13. Cập nhật `NovelController` — truyền `userRating` vào model

```java
@GetMapping("/novel/{id}")
public String detail(@PathVariable Long id, Model model, Authentication auth) {
    // ... existing code ...

    // Thêm: điểm user đã vote (nếu đã đăng nhập)
    Integer userRating = null;
    if (auth != null && auth.isAuthenticated()) {
        Long userId = getCurrentUserId(auth);
        userRating = ratingService.getUserRating(id, userId).orElse(null);
    }
    model.addAttribute("userRating", userRating);

    return "client/novel/show";
}
```

---

## 14. Thêm script tag vào `show.html`

```html
<!-- Trước </body> -->
<script th:src="@{/client/js/star-rating.js}"></script>
```

---

## 15. Luồng hoạt động đầy đủ

```
Trang detail tải
    │
    ├─ Server render: novel.ratingAvg, novel.ratingCount, userRating
    │
    ├─ JS renderAverageStars(avg)  → hiển thị sao tổng hợp
    └─ JS highlightStars(userScore) → highlight sao user đã vote (nếu có)

User hover vào sao 4
    └─ highlightStars(4) → 4 sao vàng, sao 5 trống

User rời chuột
    └─ highlightStars(userScore) → quay về trạng thái cũ

User click sao 4
    └─ POST /novel/{id}/rate?score=4
        ├─ Server: RatingService.rate() → upsert novel_rating
        ├─ Server: refreshNovelRatingCache() → cập nhật novels.rating_avg/count
        └─ Response: { success, ratingAvg, ratingCount, userScore }
            ├─ UI cập nhật sao tổng hợp + text "4.2 (129 ratings)"
            └─ Feedback: "Thanks for rating!" → tự xóa sau 3s
```

---

## 16. Test scenarios

| # | Kịch bản | Expected |
|---|---|---|
| T1 | Anonymous vào trang | Hiện điểm TB, không hiện widget đánh giá, hiện link login |
| T2 | User mới vote lần đầu (3 sao) | Score lưu vào DB, điểm TB cập nhật, 3 sao highlight |
| T3 | User vote lại (từ 3 → 5 sao) | Record update, không insert thêm, điểm TB thay đổi |
| T4 | User reload trang sau khi vote | Widget hiện đúng số sao user đã vote |
| T5 | Vote score = 0 hoặc 6 | Server trả 400 Bad Request |
| T6 | Vote khi chưa login (direct POST) | Server trả 401 |
| T7 | 1 novel có 0 vote | Hiện "—" thay vì "0.0", count hiện "(0 ratings)" |
| T8 | Novel bị xóa | `ON DELETE CASCADE` → xóa toàn bộ rating của novel đó |

---

## 17. Tóm tắt file thay đổi

| File | Loại |
|---|---|
| `domain/NovelRating.java` | NEW |
| `repository/NovelRatingRepository.java` | NEW |
| `service/RatingService.java` | NEW |
| `controller/client/RatingController.java` | NEW |
| `static/client/js/star-rating.js` | NEW |
| `domain/Novel.java` | +2 fields: `ratingAvg`, `ratingCount` |
| `controller/client/NovelController.java` | +`userRating` vào model |
| `templates/client/novel/show.html` | +rating section HTML + script tag |
| `static/client/css/novel_detail.css` | +star rating styles |
| `application.properties` | Không thay đổi |
| `pom.xml` | Không thay đổi |

**Không ảnh hưởng:** comment system, bookmark, bookshelf, TTS, read count, hot badge, reading history.
