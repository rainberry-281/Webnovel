# Thuật toán đề xuất (Recommendation) — berrynovel

> **Project:** berrynovel (Spring Boot 3.5 + Thymeleaf + MySQL)
> **Date:** 2026-05-28 (cập nhật: có thêm star rating)

---

## 1. Kiểm kê dữ liệu hiện có

Trước khi chọn thuật toán, phải xem có gì trong tay:

| Entity | Dữ liệu có thể dùng để recommend |
|---|---|
| `Novel` | `genres`, `type`, `progress`, `hot`, `totalReadCount`, `ratingAvg`, `ratingCount` |
| `Chapter` | `readCount`, `createdAt` |
| `ChapterReadLog` | `userId`, `novelId`, `chapterId`, `readAt` — **log đọc thực tế** |
| `Bookshelf` | `userId`, `novelId`, `savedAt` — **user đã lưu vào thư viện** |
| `NovelRating` | `userId`, `novelId`, `score` (1–5) — **⭐ MỚI: explicit rating** |
| `Comment` | `userId`, `novelId` — **user đã bình luận** |
| `Bookmark` | `userId`, `novelId`/`chapterId` — **user đã bookmark** |
| `User` | `id`, `role` |
| `Genre` | `id`, `name`, `status` |

### Tín hiệu — Kết hợp Implicit + Explicit feedback

| Hành động | Loại | Ý nghĩa | Trọng số |
|---|---|---|---|
| Đọc chapter (`ChapterReadLog`) | Implicit | Quan tâm mức cơ bản | ⭐ |
| Đọc nhiều chapter cùng novel | Implicit | Thích novel đó | ⭐⭐ |
| Lưu vào Bookshelf | Implicit | Muốn theo dõi | ⭐⭐⭐ |
| Bookmark chapter | Implicit | Đang đọc nghiêm túc | ⭐⭐ |
| Bình luận | Implicit | Rất quan tâm | ⭐⭐⭐ |
| **Rating 4–5 sao** | **Explicit** | **Thích rõ ràng** | **⭐⭐⭐⭐** |
| **Rating 1–2 sao** | **Explicit** | **Không thích — tín hiệu âm** | **❌ penalty** |
| **Rating 3 sao** | **Explicit** | Trung lập | (bỏ qua) |

> ✅ **Bây giờ đã có cả Explicit feedback (rating 1–5 sao).**  
> Đây là tín hiệu mạnh nhất — user bày tỏ ý kiến trực tiếp, không cần suy diễn từ hành vi.

---

## 2. Các thuật toán phù hợp — xếp hạng theo độ phù hợp

### 🥇 Lựa chọn 1 — Content-Based Filtering theo Genre (ĐỀ XUẤT TRIỂN KHAI TRƯỚC)

**Ý tưởng:** Đề xuất novel có cùng genre với những novel user đã đọc/lưu.

**Tại sao phù hợp nhất ngay lúc này:**
- Không cần nhiều user — hoạt động ngay kể cả khi DB còn ít dữ liệu
- Dữ liệu genre đã có sẵn (`novel_genre` table)
- Không cần ML, không cần thư viện ngoài
- Implement được hoàn toàn bằng SQL + Java

**Thuật toán (pseudo-SQL):**

```sql
-- Tìm các genre mà user hay đọc/lưu nhất
SELECT g.id, g.name, COUNT(*) as genre_score
FROM chapter_read_log crl
JOIN novel_genre ng ON crl.novel_id = ng.novel_id
JOIN genres g ON ng.genre_id = g.id
WHERE crl.user_id = :userId
  AND crl.read_at > NOW() - INTERVAL 30 DAY
GROUP BY g.id
ORDER BY genre_score DESC
LIMIT 5;

-- Tìm novel cùng genre, chưa đọc
SELECT DISTINCT n.id, n.title, COUNT(ng.genre_id) as match_count
FROM novels n
JOIN novel_genre ng ON n.id = ng.novel_id
WHERE ng.genre_id IN (:preferredGenreIds)
  AND n.status = true
  AND n.id NOT IN (
      SELECT DISTINCT novel_id FROM chapter_read_log WHERE user_id = :userId
  )
GROUP BY n.id
ORDER BY match_count DESC, n.total_read_count DESC
LIMIT 10;
```

**Kết quả:** "Vì bạn thích [Tiên Hiệp, Tu Tiên] → Đây là những truyện tương tự"

---

### 🥈 Lựa chọn 2 — Popularity-Based (Fallback cho anonymous user)

**Ý tưởng:** Hiển thị novel được đọc nhiều nhất trong 7/30 ngày qua.

**Phù hợp khi:** User chưa đăng nhập, hoặc user mới chưa có lịch sử đọc.

```sql
-- Top novel được đọc nhiều nhất 7 ngày qua
SELECT novel_id, COUNT(*) as read_count_7d
FROM chapter_read_log
WHERE read_at > NOW() - INTERVAL 7 DAY
GROUP BY novel_id
ORDER BY read_count_7d DESC
LIMIT 10;
```

**Ưu điểm:** Zero cold-start, luôn có kết quả, phản ánh xu hướng thực.

---

### 🥉 Lựa chọn 3 — Item-Based Collaborative Filtering (Nâng cao, khi có đủ dữ liệu)

**Ý tưởng:** "Người đọc novel A cũng hay đọc novel B" → Nếu user đọc A, recommend B.

**Tính tương đồng giữa 2 novel dựa trên user đọc chung:**

```
similarity(A, B) = |users đọc cả A và B| / sqrt(|users đọc A| × |users đọc B|)
```

Đây là **Jaccard similarity** hoặc **Cosine similarity** trên binary matrix.

**Ví dụ ma trận:**

```
           Novel1  Novel2  Novel3  Novel4
User1         1       1       0       1
User2         1       1       1       0
User3         0       1       1       0
User4         1       0       0       1
```

→ Novel1 và Novel2 có similarity cao vì cùng được User1 và User2 đọc.

**Triển khai trong MySQL:**

```sql
-- Đếm user đọc chung của 2 novel
SELECT 
    a.novel_id AS novel_a,
    b.novel_id AS novel_b,
    COUNT(DISTINCT a.user_id) AS common_readers
FROM chapter_read_log a
JOIN chapter_read_log b ON a.user_id = b.user_id
WHERE a.novel_id != b.novel_id
  AND a.user_id IS NOT NULL
GROUP BY a.novel_id, b.novel_id
HAVING common_readers >= 3  -- ngưỡng tối thiểu
ORDER BY common_readers DESC;
```

> ⚠️ **Điều kiện:** Cần ít nhất ~50–100 user có lịch sử đọc để kết quả có ý nghĩa.  
> Nếu DB còn ít user → kết quả sẽ nhiễu.

---

### 🔬 Lựa chọn 4 — Hybrid: Content + Collaborative (Tương lai)

Kết hợp cả hai:

```
final_score = α × content_score + β × collab_score + γ × popularity_score
```

Ví dụ: `α=0.5, β=0.3, γ=0.2`

Cách này cho kết quả tốt nhất nhưng cần nhiều dữ liệu và công sức triển khai.

---

## 3. Ma trận quyết định — Chọn thuật toán nào?

| Tiêu chí | Content-Based | Popularity | Item-Based CF | Hybrid |
|---|---|---|---|---|
| **Không cần nhiều user** | ✅ | ✅ | ❌ (cần ~100+ user) | ❌ |
| **Anonymous user** | ❌ (cần biết lịch sử) | ✅ | ❌ | ✅ |
| **Không cần ML/thư viện** | ✅ | ✅ | ✅ (chỉ SQL) | ⚠️ |
| **Implement nhanh** | ✅ (1–2 ngày) | ✅ (vài giờ) | ⚠️ (3–5 ngày) | ❌ (nhiều tuần) |
| **Chất lượng đề xuất** | Tốt | Trung bình | Rất tốt | Xuất sắc |
| **Phù hợp giai đoạn hiện tại** | ✅ ✅ | ✅ | ⚠️ | ❌ |

---

## 4. Kiến trúc đề xuất — Implement theo giai đoạn

### Giai đoạn 1 (Làm ngay) — Popularity + Content-Based

```
Trang chủ:
├── "Đang hot" → Popularity (7 ngày, dựa trên chapter_read_log)  [đã có]
├── "Mới cập nhật" → Sort by chapter.createdAt DESC              [đã có]
└── "Có thể bạn thích" → [CHỈ HIỆN KHI ĐÃ ĐĂNG NHẬP]
    └── Content-Based by genre từ lịch sử đọc/bookshelf

Trang novel detail:
└── "Truyện tương tự" → Novel cùng genre, sort by totalReadCount
```

### Giai đoạn 2 (Khi có ~50+ active user) — Item-Based CF

```
Trang novel detail:
└── "Người đọc truyện này cũng đọc..." → Item-Based CF

Profile/Trang cá nhân:
└── "Đề xuất cho bạn" → Hybrid (content + CF)
```

---

## 5. Implementation — Giai đoạn 1 (Spring Boot)

### 5.1 `RecommendationService.java`

```java
@Service
public class RecommendationService {

    private final ChapterReadLogRepository readLogRepo;
    private final BookshelfRepository bookshelfRepo;
    private final NovelRepository novelRepo;

    // ─── 1. Novels cùng genre với novel hiện tại (trang detail) ───────────
    public List<Novel> getSimilarNovels(Long novelId, int limit) {
        // Lấy genre của novel hiện tại
        // Query novels cùng genre, loại trừ novelId, sort by totalReadCount
        // Implement bằng JPQL hoặc Specification
    }

    // ─── 2. Đề xuất dựa trên lịch sử đọc của user (logged-in) ────────────
    public List<Novel> getPersonalizedRecommendations(Long userId, int limit) {
        // Lấy genre user hay đọc nhất (từ chapter_read_log + bookshelf)
        // Query novel cùng genre, chưa đọc, sort by totalReadCount
    }

    // ─── 3. Fallback: Popular novels (anonymous hoặc new user) ───────────
    public List<Novel> getPopularNovels(int days, int limit) {
        // GROUP BY novel_id từ chapter_read_log WHERE read_at > NOW() - days
        // Sort by count DESC
    }
}
```

### 5.2 SQL queries cụ thể cho MySQL

#### Query lấy genre ưa thích của user:

```sql
SELECT ng.genre_id, COUNT(*) as score
FROM chapter_read_log crl
JOIN novel_genre ng ON crl.novel_id = ng.novel_id
WHERE crl.user_id = ?
  AND crl.read_at >= DATE_SUB(NOW(), INTERVAL 60 DAY)
GROUP BY ng.genre_id

UNION ALL

SELECT ng.genre_id, COUNT(*) * 3 as score  -- bookshelf có trọng số cao hơn
FROM Bookshelf b
JOIN novel_genre ng ON b.novel_id = ng.novel_id
WHERE b.user_id = ?
GROUP BY ng.genre_id

ORDER BY score DESC
LIMIT 5;
```

#### Query novel đề xuất (chưa đọc, cùng genre):

```sql
SELECT n.*, COUNT(ng.genre_id) as genre_match
FROM Novels n
JOIN novel_genre ng ON n.id = ng.novel_id
WHERE ng.genre_id IN (?)          -- preferred genre ids
  AND n.status = true
  AND n.id NOT IN (               -- loại novel đã đọc
      SELECT DISTINCT novel_id FROM chapter_read_log WHERE user_id = ?
  )
  AND n.id NOT IN (               -- loại novel đã lưu (đã biết rồi)
      SELECT novel_id FROM Bookshelf WHERE user_id = ?
  )
GROUP BY n.id
ORDER BY genre_match DESC, n.total_read_count DESC
LIMIT 10;
```

### 5.3 Controller — thêm vào trang chủ & detail

```java
// HomeController.java
@GetMapping("/")
public String home(Model model, Authentication auth) {
    // Existing code...
    
    if (auth != null && auth.isAuthenticated()) {
        Long userId = getCurrentUserId(auth);
        List<Novel> forYou = recommendService.getPersonalizedRecommendations(userId, 6);
        model.addAttribute("forYouNovels", forYou);
    } else {
        List<Novel> popular = recommendService.getPopularNovels(7, 6);
        model.addAttribute("popularNovels", popular);
    }
    return "client/home/index";
}

// NovelController.java — trang detail
@GetMapping("/novel/{id}")
public String detail(@PathVariable Long id, Model model) {
    // Existing code...
    List<Novel> similar = recommendService.getSimilarNovels(id, 6);
    model.addAttribute("similarNovels", similar);
    return "client/novel/show";
}
```

### 5.4 Thymeleaf — section hiển thị

```html
<!-- Section "Truyện tương tự" trên trang detail -->
<section th:if="${not #lists.isEmpty(similarNovels)}">
    <h3>Truyện tương tự</h3>
    <div class="novel-grid">
        <div th:each="n : ${similarNovels}" class="novel-card">
            <a th:href="@{/novel/{id}(id=${n.id})}">
                <img th:src="@{/images/novel/{img}(img=${n.image})}" th:alt="${n.title}"/>
                <span th:text="${n.title}"></span>
            </a>
        </div>
    </div>
</section>

<!-- Section "Có thể bạn thích" trên trang chủ (logged in) -->
<section sec:authorize="isAuthenticated()" 
         th:if="${not #lists.isEmpty(forYouNovels)}">
    <h3>Có thể bạn thích</h3>
    <!-- novel cards -->
</section>
```

---

## 6. Scoring tổng hợp — Cập nhật có Rating

### 6.1 Hàm score novel để recommend (Java)

```java
/**
 * Tính điểm recommend cho một novel.
 * Kết hợp: genre match + user-specific rating history + popularity + quality.
 */
double scoreNovel(
        Novel novel,
        List<Long> preferredGenreIds,      // genre user hay đọc/vote cao
        Map<Long, Long> novelReadCounts7d, // lượt đọc 7 ngày
        Map<Long, Integer> userRatingMap   // novelId → score user đã vote (nếu có)
) {
    // ── 1. Genre match (0–30 điểm) ──────────────────────────────────────
    long genreMatches = novel.getGenres().stream()
        .filter(g -> preferredGenreIds.contains(g.getId()))
        .count();
    double genreScore = genreMatches * 10.0;

    // ── 2. Rating quality của novel (0–20 điểm) ──────────────────────────
    // Chỉ tính khi có đủ lượt vote (tránh novel 1 vote 5 sao xếp đầu)
    double ratingScore = 0.0;
    if (novel.getRatingCount() >= 5) {
        // Bayesian average: kéo về mức trung bình (3.0) nếu ít vote
        double bayesianAvg = (novel.getRatingAvg() * novel.getRatingCount() + 3.0 * 10)
                           / (novel.getRatingCount() + 10);
        ratingScore = (bayesianAvg - 3.0) * 10.0; // -20 đến +20
    }

    // ── 3. Popularity gần đây (0–10 điểm) ───────────────────────────────
    long recentReads = novelReadCounts7d.getOrDefault(novel.getId(), 0L);
    double popularityScore = Math.min(recentReads * 0.5, 10.0); // cap tại 10

    // ── 4. Tổng độ phổ biến tích lũy (0–5 điểm, log scale) ─────────────
    double totalPopularity = Math.min(Math.log1p(novel.getTotalReadCount()), 5.0);

    return genreScore + ratingScore + popularityScore + totalPopularity;
}
```

### 6.2 Lấy preferred genre từ cả hành vi lẫn rating (SQL)

```sql
-- Genre ưa thích của user, kết hợp 3 nguồn tín hiệu
SELECT ng.genre_id, SUM(signal_score) AS total_score
FROM (

    -- Nguồn 1: Đọc chapter (implicit, thấp)
    SELECT crl.novel_id, COUNT(*) * 1 AS signal_score
    FROM chapter_read_log crl
    WHERE crl.user_id = :userId
      AND crl.read_at >= DATE_SUB(NOW(), INTERVAL 60 DAY)
    GROUP BY crl.novel_id

    UNION ALL

    -- Nguồn 2: Lưu vào Bookshelf (implicit, trung bình)
    SELECT b.novel_id, 3 AS signal_score
    FROM Bookshelf b WHERE b.user_id = :userId

    UNION ALL

    -- Nguồn 3: Rating (explicit — mạnh nhất)
    SELECT nr.novel_id,
           CASE
               WHEN nr.score >= 4 THEN 5   -- thích rõ ràng → +5
               WHEN nr.score =  3 THEN 0   -- trung lập → bỏ qua
               ELSE -3                     -- không thích → tín hiệu âm
           END AS signal_score
    FROM novel_rating nr WHERE nr.user_id = :userId

) AS signals
JOIN novel_genre ng ON signals.novel_id = ng.novel_id
GROUP BY ng.genre_id
HAVING total_score > 0        -- loại genre bị vote thấp nhiều
ORDER BY total_score DESC
LIMIT 5;
```

### 6.3 Lọc novel đã bị user rate thấp khỏi kết quả

```sql
-- Thêm điều kiện NOT IN trong query recommend:
AND n.id NOT IN (
    SELECT novel_id FROM novel_rating
    WHERE user_id = :userId AND score <= 2   -- loại novel đã bị chê
)
```

### 6.4 Sort kết quả ưu tiên novel chất lượng cao

```sql
ORDER BY
    genre_match DESC,
    -- Bayesian average (chỉ áp dụng khi ≥ 5 vote)
    CASE
        WHEN n.rating_count >= 5
        THEN (n.rating_avg * n.rating_count + 3.0 * 10) / (n.rating_count + 10)
        ELSE 3.0
    END DESC,
    n.total_read_count DESC
LIMIT 10;
```

---

## 7. Xử lý Cold Start (user mới / anonymous)

| Trạng thái user | Chiến lược |
|---|---|
| **Chưa đăng nhập** | Popularity (7 ngày) + novel có `rating_avg ≥ 4.0` |
| **Mới đăng nhập, chưa đọc, chưa vote** | Hot + Mới cập nhật + Top rated (`rating_avg DESC`) |
| **Đã đọc 1–2 novel, chưa vote** | Content-based từ genre |
| **Đã vote ít nhất 1 novel** | Dùng rating làm tín hiệu genre ngay lập tức |
| **Đã đọc 5+ novel** | Content-based đầy đủ + rating filter |
| **User tích cực (20+ novel, 5+ votes)** | Collaborative Filtering + rating-aware scoring |

> 💡 **Rating giải quyết Cold Start một phần:** User mới chưa đọc nhiều nhưng đã vote
> 1–2 novel → ta đã biết sở thích genre ngay lập tức, không cần chờ tích lũy log đọc.

---

## 8. Performance & Caching

Recommendation query có thể nặng nếu DB lớn. Giải pháp:

```java
// Cache kết quả 30 phút với Spring Cache
@Cacheable(value = "recommendations", key = "#userId")
public List<Novel> getPersonalizedRecommendations(Long userId, int limit) { ... }

@CacheEvict(value = "recommendations", key = "#userId")
public void invalidateCache(Long userId) { ... }

// Popular novels: cache 1 giờ (ít thay đổi)
@Cacheable(value = "popularNovels", key = "#days + '_' + #limit")
public List<Novel> getPopularNovels(int days, int limit) { ... }
```

Thêm vào `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

---

## 9. Tóm tắt — Roadmap triển khai

```
✅ Bước 1: getSimilarNovels(novelId)
           Trang detail "Truyện tương tự" — cùng genre, sort by rating_avg + totalReadCount

✅ Bước 2: getPopularNovels(7 days)
           Fallback anonymous — top read + filter rating_avg >= 3.5 nếu đủ vote

✅ Bước 3: getPersonalizedRecommendations() — rating-aware
           Trang chủ user đăng nhập
           Tín hiệu: chapter_read_log + Bookshelf + novel_rating (score >= 4)
           Lọc: loại novel user đã vote <= 2 sao
           Sort: scoreNovel() tích hợp Bayesian rating

✅ Bước 4: getTopRatedNovels()
           Section "Đánh giá cao" trên trang chủ / category
           WHERE rating_count >= 10 ORDER BY bayesian_avg DESC

⏳ Bước 5: Item-Based CF + rating matrix      → Khi có 50+ active user
           Dùng novel_rating làm ma trận User×Novel thay vì chỉ đọc/không đọc
           → Cosine similarity trên vector rating (1–5) chính xác hơn binary

⏳ Bước 6: Rating-weighted Collaborative CF   → Giai đoạn nâng cao
           Người rate 5 sao novel A và B có "gu" giống nhau hơn
           người chỉ đọc cả hai
```

---

## 10. Kết luận

Với việc đã có star rating, **thuật toán đề xuất được nâng cấp lên:**

> **Rating-Aware Content-Based Filtering + Bayesian Quality Score + Popularity fallback**

### Thay đổi so với phiên bản trước

| Khía cạnh | Trước (chỉ implicit) | Sau (có rating) |
|---|---|---|
| **Tín hiệu mạnh nhất** | Bookshelf save | Rating 4–5 sao |
| **Loại novel không thích** | Chỉ loại đã đọc | Loại cả novel bị vote 1–2 sao |
| **Chất lượng novel** | Chỉ dùng `totalReadCount` | Bayesian average của `rating_avg` |
| **Sort kết quả** | `match_count + totalReadCount` | `match_count + bayesian_avg + recentReads` |
| **Cold start** | Chờ tích lũy log đọc | Vote 1 novel → biết ngay sở thích |
| **Top rated section** | Không có | Novel rating_avg ≥ 4.0, count ≥ 10 |

### Nguyên tắc Bayesian Rating (quan trọng)

Không dùng `rating_avg` thô để sort vì:
- Novel A: 5.0 sao — 1 người vote → không đáng tin
- Novel B: 4.2 sao — 200 người vote → đáng tin hơn nhiều

Dùng **Bayesian average** thay thế:
```
bayesian_avg = (avg × count + global_mean × m) / (count + m)
```
Với `global_mean = 3.0` (điểm trung bình hệ thống), `m = 10` (ngưỡng tin cậy).

**Tránh làm ngay:** Matrix Factorization, ALS, Neural CF — vẫn chưa cần thiết.
Rating-aware content-based đã đủ hiệu quả và maintain được.
