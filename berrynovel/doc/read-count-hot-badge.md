# Read Count & Hot Badge — Implementation Spec

> **Project:** berrynovel (Spring Boot + Thymeleaf + MySQL)
> **Date:** 2026-05-19

---

## 1. Current State Analysis

### Existing `Novel.java`
```java
@Enumerated(EnumType.STRING)
private NovelHot hot = NovelHot.NOT_HOT;  // manually set by admin: HOT | NOT_HOT
```

### Existing `NovelHot.java`
```java
public enum NovelHot {
    NOT_HOT("No"),
    HOT("Yes");
}
```

**Problem:** `hot` is currently set **manually by admin** in the admin panel.
The goal is to make it **auto-calculated from read counts** while keeping
the admin field (`hot`) visible in the admin UI for manual override if needed.

### Existing `Chapter.java`
Has: `id`, `title`, `image`, `content`, `createdAt`, `novel`.
**Missing:** `readCount`.

### Existing `Novel.java`
Has: `id`, `title`, `author`, `description`, `type`, `progress`, `status`,
`hot` (NovelHot enum), `image`, `createdAt`, `user`, `genres`.
**Missing:** `totalReadCount`.

### Existing read flow
```
GET /reader/{novelId}/{chapterId}
→ ReaderController.getReaderPage()
→ renders show.html
```
No read tracking at all currently.

---

## 2. Architecture Overview

```
User opens chapter page
        │
        ▼
ReaderController.getReaderPage()
        │
        ├─ (existing) load chapter, novel, prev/next, isBookmarked
        │
        └─ (NEW) readCountService.recordChapterRead(novelId, chapterId, auth, request)
                    │
                    ├─ Check cooldown via ChapterReadLog table
                    │   ├─ logged-in user  → by userId + chapterId + 30-min window
                    │   └─ anonymous user  → by sessionId/ipHash + chapterId + 30-min window
                    │
                    ├─ If valid read:
                    │   ├─ UPDATE Chapters SET read_count = read_count + 1
                    │   ├─ UPDATE Novels  SET total_read_count = total_read_count + 1
                    │   └─ INSERT ChapterReadLog
                    │
                    └─ If cooldown active → do nothing
```

---

## 3. Database / Schema Changes

### 3.1 Migration SQL

```sql
-- V3__add_read_count.sql

-- Add read count to Chapters
ALTER TABLE Chapters
    ADD COLUMN read_count BIGINT NOT NULL DEFAULT 0 COMMENT 'Read count for this chapter';

-- Add total read count to Novels
ALTER TABLE Novels
    ADD COLUMN total_read_count BIGINT NOT NULL DEFAULT 0 COMMENT 'Cumulative read count across all chapters';

-- New tracking table for cooldown deduplication
CREATE TABLE chapter_read_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    chapter_id    BIGINT       NOT NULL,
    novel_id      BIGINT       NOT NULL,
    user_id       BIGINT       NULL     COMMENT 'NULL for anonymous users',
    session_id    VARCHAR(128) NULL     COMMENT 'HTTP session ID for anonymous users',
    ip_hash       VARCHAR(64)  NULL     COMMENT 'SHA-256 hash of IP address',
    user_agent_hash VARCHAR(64) NULL    COMMENT 'SHA-256 hash of User-Agent string',
    read_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_crl_chapter_user    (chapter_id, user_id, read_at),
    INDEX idx_crl_chapter_session (chapter_id, session_id, read_at),
    INDEX idx_crl_read_at         (read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Backfill: set total_read_count from existing chapter read counts (all zero initially)
-- UPDATE Novels n SET n.total_read_count = (
--     SELECT COALESCE(SUM(c.read_count), 0) FROM Chapters c WHERE c.novel_id = n.id
-- );
```

### 3.2 Schema after changes

```
Chapters {
    id            : BIGINT PK
    title         : VARCHAR
    image         : VARCHAR
    content       : LONGTEXT
    created_at    : DATETIME
    novel_id      : BIGINT FK
    read_count    : BIGINT DEFAULT 0    ← NEW
}

Novels {
    id                : BIGINT PK
    title             : VARCHAR
    author            : VARCHAR
    description       : TEXT
    type              : ENUM
    progress          : ENUM
    status            : BOOLEAN
    hot               : ENUM (HOT | NOT_HOT)   ← existing, keep for manual override
    image             : VARCHAR
    created_at        : DATETIME
    user_id           : BIGINT FK
    total_read_count  : BIGINT DEFAULT 0        ← NEW
}

chapter_read_log {
    id              : BIGINT PK AUTO_INCREMENT
    chapter_id      : BIGINT NOT NULL
    novel_id        : BIGINT NOT NULL
    user_id         : BIGINT NULL
    session_id      : VARCHAR(128) NULL
    ip_hash         : VARCHAR(64)  NULL
    user_agent_hash : VARCHAR(64)  NULL
    read_at         : DATETIME NOT NULL
}
```

---

## 4. New Entity: `ChapterReadLog.java`

```java
package com.bn.berrynovel.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_read_log",
    indexes = {
        @Index(name = "idx_crl_chapter_user",    columnList = "chapter_id, user_id, read_at"),
        @Index(name = "idx_crl_chapter_session", columnList = "chapter_id, session_id, read_at"),
        @Index(name = "idx_crl_read_at",         columnList = "read_at")
    }
)
public class ChapterReadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "user_id")
    private Long userId;                  // null for anonymous

    @Column(name = "session_id", length = 128)
    private String sessionId;             // HTTP session ID

    @Column(name = "ip_hash", length = 64)
    private String ipHash;                // SHA-256 of remote IP

    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;         // SHA-256 of User-Agent

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    // getters & setters ...
}
```

---

## 5. Modified Entities

### 5.1 `Chapter.java` — add `readCount`

```java
@Column(name = "read_count", nullable = false)
private Long readCount = 0L;

public Long getReadCount() { return readCount; }
public void setReadCount(Long readCount) { this.readCount = readCount; }
```

### 5.2 `Novel.java` — add `totalReadCount`

```java
@Column(name = "total_read_count", nullable = false)
private Long totalReadCount = 0L;

public Long getTotalReadCount() { return totalReadCount; }
public void setTotalReadCount(Long totalReadCount) { this.totalReadCount = totalReadCount; }
```

> **Keep the existing `hot` (NovelHot enum) field.** It remains available for
> manual admin override. The Hot badge in templates will be driven by
> `totalReadCount >= threshold`, NOT by the enum. The enum can optionally be
> synced by a scheduled job later.

---

## 6. New Repository: `ChapterReadLogRepository.java`

```java
package com.bn.berrynovel.repository;

import com.bn.berrynovel.domain.ChapterReadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChapterReadLogRepository extends JpaRepository<ChapterReadLog, Long> {

    // Cooldown check for logged-in users
    Optional<ChapterReadLog> findFirstByChapterIdAndUserIdAndReadAtAfter(
            Long chapterId, Long userId, LocalDateTime since);

    // Cooldown check for anonymous users by session
    Optional<ChapterReadLog> findFirstByChapterIdAndSessionIdAndReadAtAfter(
            Long chapterId, String sessionId, LocalDateTime since);

    // Cleanup old logs (for scheduled maintenance)
    void deleteByReadAtBefore(LocalDateTime cutoff);

    // Count reads per novel in last N days (for trending feature)
    @Query("SELECT COUNT(l) FROM ChapterReadLog l WHERE l.novelId = :novelId AND l.readAt >= :since")
    long countByNovelIdAndReadAtAfter(Long novelId, LocalDateTime since);
}
```

### Updated `ChapterRepository.java` — atomic increment

```java
// Atomic increment to avoid race conditions
@Modifying
@Query("UPDATE Chapter c SET c.readCount = c.readCount + 1 WHERE c.id = :chapterId")
void incrementReadCount(@Param("chapterId") Long chapterId);
```

### Updated `NovelRepository.java` — atomic increment + sort query

```java
// Atomic increment
@Modifying
@Query("UPDATE Novel n SET n.totalReadCount = n.totalReadCount + 1 WHERE n.id = :novelId")
void incrementTotalReadCount(@Param("novelId") Long novelId);

// Sort by most-read (for ranking page)
List<Novel> findByStatusTrueOrderByTotalReadCountDesc(Pageable pageable);
```

---

## 7. New Service: `ReadCountService.java`

```java
package com.bn.berrynovel.service;

import com.bn.berrynovel.domain.ChapterReadLog;
import com.bn.berrynovel.repository.ChapterReadLogRepository;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.repository.NovelRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class ReadCountService {

    private final ChapterReadLogRepository logRepository;
    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;

    /** Cooldown in minutes — configurable in application.properties */
    @Value("${read.count.cooldown-minutes:30}")
    private int cooldownMinutes;

    public ReadCountService(ChapterReadLogRepository logRepository,
                            ChapterRepository chapterRepository,
                            NovelRepository novelRepository) {
        this.logRepository  = logRepository;
        this.chapterRepository = chapterRepository;
        this.novelRepository   = novelRepository;
    }

    /**
     * Record a chapter read if the cooldown has expired.
     * Called from ReaderController on every GET /reader/{novelId}/{chapterId}.
     */
    @Transactional
    public void recordChapterRead(Long novelId, Long chapterId,
                                  Authentication authentication,
                                  HttpServletRequest request) {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(cooldownMinutes);

            if (isLoggedIn(authentication)) {
                // --- Logged-in user: deduplicate by userId ---
                Long userId = resolveUserId(authentication);
                boolean alreadyCounted = logRepository
                        .findFirstByChapterIdAndUserIdAndReadAtAfter(chapterId, userId, since)
                        .isPresent();
                if (alreadyCounted) return;

                incrementCounts(novelId, chapterId);
                saveLog(chapterId, novelId, userId, null, request);

            } else {
                // --- Anonymous user: deduplicate by sessionId ---
                String sessionId = request.getSession(true).getId();
                boolean alreadyCounted = logRepository
                        .findFirstByChapterIdAndSessionIdAndReadAtAfter(chapterId, sessionId, since)
                        .isPresent();
                if (alreadyCounted) return;

                incrementCounts(novelId, chapterId);
                saveLog(chapterId, novelId, null, sessionId, request);
            }

        } catch (Exception e) {
            // Read-count failure must never break the reader page
            // log the error but swallow it
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void incrementCounts(Long novelId, Long chapterId) {
        chapterRepository.incrementReadCount(chapterId);
        novelRepository.incrementTotalReadCount(novelId);
    }

    private void saveLog(Long chapterId, Long novelId,
                         Long userId, String sessionId,
                         HttpServletRequest request) {
        ChapterReadLog log = new ChapterReadLog();
        log.setChapterId(chapterId);
        log.setNovelId(novelId);
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setIpHash(hashString(getClientIp(request)));
        log.setUserAgentHash(hashString(request.getHeader("User-Agent")));
        log.setReadAt(LocalDateTime.now());
        logRepository.save(log);
    }

    private boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
    }

    private Long resolveUserId(Authentication auth) {
        // Assumes UserDetails principal has getId() or is a User entity
        // Adjust based on your CustomUserDetailsService implementation
        Object principal = auth.getPrincipal();
        if (principal instanceof com.bn.berrynovel.domain.User user) {
            return user.getId();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** SHA-256 hash — avoids storing raw IP/UA */
    private String hashString(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }
}
```

---

## 8. Modified Controller: `ReaderController.java`

Add one call to `readCountService.recordChapterRead()`:

```java
// Inject ReadCountService
private final ReadCountService readCountService;

@GetMapping("/{novelID}/{chapterID}")
public String getReaderPage(@PathVariable Long novelID, @PathVariable Long chapterID,
        Model model, Authentication authentication, HttpServletRequest request) {

    // ... existing code unchanged ...

    // ← NEW: record this read (non-blocking, swallows exceptions internally)
    readCountService.recordChapterRead(novelID, chapterID, authentication, request);

    // ... rest of existing code unchanged ...
    return "/client/reader/show";
}
```

---

## 9. Configuration: `application.properties`

```properties
# Read count cooldown (minutes) per user/session per chapter
read.count.cooldown-minutes=30

# Hot badge threshold — novels with totalReadCount >= this are "Hot"
hot.novel.read-threshold=1000
```

---

## 10. Hot Badge Logic

### 10.1 Strategy: Fixed Threshold (Option A — start here)

A novel is displayed as **Hot** if:
```
novel.totalReadCount >= hot.novel.read-threshold   (default: 1000)
```

**No changes to the `hot` enum field or admin panel** — the enum stays for
manual admin override. The badge in templates uses `totalReadCount` directly.

### 10.2 Controller — pass threshold to model

In every controller that renders novel lists, inject and pass the threshold:

```java
@Value("${hot.novel.read-threshold:1000}")
private long hotReadThreshold;

// In model population:
model.addAttribute("hotThreshold", hotReadThreshold);
```

Controllers to update:
- `HomepageController`
- `ClientNovelController` (novel detail + search)
- `ReaderController` (reader page)

---

## 11. Templates — Hot Badge & Read Count Display

### 11.1 Novel card (homepage, category, search)

```html
<!-- Novel cover card — add badge next to HOT span -->
<div class="hotnovel">
    <!-- Existing manual HOT badge (keep as-is) -->
    <span th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"
          class="badge bg-danger">HOT</span>

    <!-- NEW: auto hot badge from read count -->
    <span th:if="${novel.totalReadCount != null and novel.totalReadCount >= hotThreshold}"
          class="badge bg-warning text-dark ms-1">🔥 Hot</span>
</div>
```

### 11.2 Novel detail page `novel/show.html`

```html
<!-- Inside info-card, after Progress and Last Updated rows -->
<div class="col-md-6">
    <strong>Reads:</strong>
    <span th:text="${#numbers.formatInteger(novel.totalReadCount ?: 0, 1, 'COMMA')}">0</span>
</div>

<!-- Hot badge in header area -->
<span th:if="${novel.totalReadCount != null and novel.totalReadCount >= hotThreshold}"
      class="badge bg-warning text-dark">🔥 Hot</span>
```

### 11.3 Reader page `reader/show.html`

No badge needed here — optional chapter read count display only:
```html
<!-- Optional: show chapter read count in sidebar chapter list -->
<small th:if="${chap.readCount != null and chap.readCount > 0}"
       class="text-muted ms-1"
       th:text="'(' + ${#numbers.formatInteger(chap.readCount, 1, 'COMMA')} + ' reads)'">
</small>
```

### 11.4 Bookshelf page `library/bookshelf.html`

No badge change needed — the `novel.hot` enum badge already shows in the
novel detail/home pages that link here.

---

## 12. Sorting: Most-Read Novels

### 12.1 `NovelService.java` — new method

```java
public List<Novel> getMostReadNovels(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return novelRepository.findByStatusTrueOrderByTotalReadCountDesc(pageable);
}
```

### 12.2 Homepage controller — add Most Read section (optional)

```java
model.addAttribute("mostReadNovels", novelService.getMostReadNovels(6));
```

### 12.3 Category/Search — add sort option

```html
<select name="sort" class="form-select form-select-sm" onchange="this.form.submit()">
    <option value="latest"   th:selected="${sort == 'latest'}">Latest</option>
    <option value="most-read" th:selected="${sort == 'most-read'}">Most Read</option>
</select>
```

---

## 13. Files to Create / Modify

### New Files

| File | Description |
|---|---|
| `domain/ChapterReadLog.java` | New entity for tracking reads |
| `repository/ChapterReadLogRepository.java` | Repository for ChapterReadLog |
| `service/ReadCountService.java` | Core read-counting logic |
| `db/migration/V3__add_read_count.sql` | DB migration |

### Modified Files

| File | Change |
|---|---|
| `domain/Chapter.java` | + `readCount` field (Long, default 0) |
| `domain/Novel.java` | + `totalReadCount` field (Long, default 0) |
| `repository/ChapterRepository.java` | + `@Modifying incrementReadCount()` |
| `repository/NovelRepository.java` | + `@Modifying incrementTotalReadCount()`, + `findByStatusTrueOrderByTotalReadCountDesc()` |
| `controller/client/ReaderController.java` | + inject `ReadCountService`, call `recordChapterRead()` |
| `controller/client/HomepageController.java` | + inject `hotReadThreshold`, add to model |
| `controller/client/ClientNovelController.java` | + inject `hotReadThreshold`, add to model |
| `templates/client/homepage/show.html` | + auto Hot badge on novel cards |
| `templates/client/novel/show.html` | + Reads count display + auto Hot badge |
| `templates/client/category/show.html` | + auto Hot badge on novel cards |
| `resources/application.properties` | + `read.count.cooldown-minutes`, `hot.novel.read-threshold` |

---

## 14. Full Flow Diagram

### 14.1 Read counting flow

```
GET /reader/1/15
        │
        ▼
ReaderController.getReaderPage(novelId=1, chapterId=15, auth, request)
        │
        ├─ [existing] load chapter, novel, prev/next, isBookmarked
        │
        └─ readCountService.recordChapterRead(1, 15, auth, request)
                │
                ├─ isLoggedIn? YES
                │   ├─ userId = auth.getPrincipal().getId()
                │   ├─ SELECT * FROM chapter_read_log
                │   │   WHERE chapter_id=15 AND user_id=42
                │   │   AND read_at > (now - 30min)  → empty
                │   ├─ UPDATE Chapters SET read_count = read_count + 1 WHERE id=15
                │   ├─ UPDATE Novels  SET total_read_count = total_read_count + 1 WHERE id=1
                │   └─ INSERT chapter_read_log (chapter_id=15, novel_id=1, user_id=42, read_at=now)
                │
                └─ isLoggedIn? NO
                    ├─ sessionId = request.getSession(true).getId()
                    ├─ SELECT * FROM chapter_read_log
                    │   WHERE chapter_id=15 AND session_id='abc123'
                    │   AND read_at > (now - 30min)  → empty
                    ├─ UPDATE Chapters SET read_count = read_count + 1 WHERE id=15
                    ├─ UPDATE Novels  SET total_read_count = total_read_count + 1 WHERE id=1
                    └─ INSERT chapter_read_log (session_id='abc123', ip_hash=..., ...)
```

### 14.2 Hot badge display flow

```
Novel detail page loaded
        │
        ├─ novel.totalReadCount = 1,245
        ├─ hotThreshold = 1,000  (from application.properties)
        │
        └─ 1245 >= 1000 → TRUE
              │
              └─ <span class="badge bg-warning">🔥 Hot</span> SHOWN
```

---

## 15. Edge Cases & Handling

| Case | Handling |
|---|---|
| `readCount` / `totalReadCount` is `null` (old data) | Default 0L in entity; templates use `?: 0` |
| Exception in ReadCountService | Wrapped in try-catch — **reader page never fails** |
| Race condition: 2 users read same chapter simultaneously | `@Modifying @Query` uses atomic SQL `UPDATE ... SET count = count + 1` |
| Bot/crawler (no session) | Falls through anonymous path; session created but cooldown applies |
| Chapter deleted after read logged | `chapter_id` in log is a plain Long, no FK constraint — log survives |
| Concurrent duplicate writes to log | DB may have 2 log rows in edge case — count increment is still atomic; 1 extra log row is acceptable |
| Very high traffic | `chapter_read_log` table grows fast → add scheduled cleanup job: `deleteByReadAtBefore(30_DAYS_AGO)` |
| Missing session ID | `request.getSession(true)` always creates a session — no null |
| `hotThreshold` not in properties | Default `1000` via `@Value("${hot.novel.read-threshold:1000}")` |
| Invalid chapter ID | ReaderController already throws before reaching ReadCountService |

---

## 16. Test Cases

```
1. First read (logged-in)
   → chapter.readCount++, novel.totalReadCount++, log inserted

2. Refresh within 30 min (logged-in)
   → cooldown active → no increment, no new log

3. Read again after 30 min (logged-in)
   → cooldown expired → increment again

4. Same user reads a different chapter
   → each chapter incremented independently

5. Anonymous user first read
   → by sessionId → incremented

6. Anonymous user refreshes
   → same sessionId within cooldown → no increment

7. novel.totalReadCount reaches hotThreshold
   → Hot badge appears in templates (no server restart needed)

8. novel.totalReadCount < hotThreshold
   → Hot badge not shown

9. ReadCountService throws exception
   → reader page still renders normally (exception swallowed)

10. Two concurrent requests for same chapter+user
    → atomic SQL UPDATE prevents double-counting in DB
```

---

## 17. How to Change the Hot Threshold

Edit `application.properties` — **no code change, no redeploy of compiled classes**:

```properties
# Increase threshold:
hot.novel.read-threshold=5000

# Lower threshold for testing:
hot.novel.read-threshold=10
```

The `@Value` annotation re-reads on Spring context refresh (hot reload with
devtools) or on restart.

---

## 18. Implementation Order (Recommended)

1. Run `V3__add_read_count.sql` migration
2. Add `readCount` to `Chapter.java`, `totalReadCount` to `Novel.java`
3. Create `ChapterReadLog.java`
4. Create `ChapterReadLogRepository.java`
5. Add `@Modifying` queries to `ChapterRepository` and `NovelRepository`
6. Create `ReadCountService.java`
7. Inject `ReadCountService` into `ReaderController`, add one line call
8. Add `hotReadThreshold` to `application.properties`
9. Inject threshold into model in `HomepageController`, `ClientNovelController`, `ReaderController`
10. Update templates: Hot badge + Reads display
11. Test: verify cooldown, verify badge threshold, verify page never breaks

---

## 19. Not Changed

- Existing `hot` enum field on `Novel` and admin panel toggle
- All routes, URL patterns, existing controller logic
- Bookmark system (`LibraryService`, `BookmarkRepository`)
- Chapter content display
- Thymeleaf expressions for novel/chapter titles (dynamic DB data)
- CSS class names, IDs
