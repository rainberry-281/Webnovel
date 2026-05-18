# Hot Tag Conflict Fix — Implementation Spec

> **Project:** berrynovel (Spring Boot + Thymeleaf + MySQL)
> **Depends on:** `read-count-hot-badge.md` (read-count system must be implemented first)
> **Date:** 2026-05-19

---

## 1. Problem Statement

The project has **two separate Hot tag systems** that now conflict:

| System | Source of truth | Where it shows |
|---|---|---|
| **Old (manual)** | `Novel.hot` = `NovelHot` enum (HOT / NOT_HOT), set by admin manually | Templates: `novel.hot.name() == 'HOT'` |
| **New (auto)** | `novel.totalReadCount >= hotThreshold` (from `read-count-hot-badge.md`) | Templates: `novel.totalReadCount >= hotThreshold` |

**Result:** A novel that has `hot = HOT` AND `totalReadCount >= 1000` will display **two "Hot" badges** side by side:

```html
[HOT] [🔥 Hot]   ← WRONG — both appear at once
```

**Goal:** Remove the old badge from templates. Keep the old DB field for admin stat purposes. Use only the new read-count badge for UI.

---

## 2. Full Inventory of Old Hot Tag Usage

### 2.1 Templates (HTML/Thymeleaf)

| File | Line | Old usage |
|---|---|---|
| `client/homepage/show.html` | 67, 97, 127 | `th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"` |
| `client/category/show.html` | 129 | `th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"` |
| `admin/novel/show.html` | 86 | `th:text="${novel.hot.displayName}"` (table column) |
| `admin/novel/create.html` | 115–128 | `<select th:field="*{hot}">` dropdown |
| `admin/novel/update.html` | 108–119 | `<select th:field="*{hot}">` dropdown |
| `admin/dashboard/show.html` | 96–98 | `${hotNovelCount}` stat card |

### 2.2 Java — Entity / Domain

| File | Line | Usage |
|---|---|---|
| `domain/Novel.java` | 50 | `private NovelHot hot = NovelHot.NOT_HOT;` |
| `domain/Novel.java` | 148–154 | `getHot()`, `setHot()` |
| `domain/NovelHot.java` | entire file | `enum NovelHot { NOT_HOT, HOT }` |

### 2.3 Java — Repository

| File | Line | Usage |
|---|---|---|
| `repository/NovelRepository.java` | 31 | `long countByHot(NovelHot hot)` |
| `repository/NovelRepository.java` | 33 | `Page<Novel> findByHot(NovelHot hot, Pageable pageable)` |
| `repository/NovelRepository.java` | 35 | `findByHotAndTitleContainingIgnoreCase(...)` |

### 2.4 Java — Service

| File | Lines | Usage |
|---|---|---|
| `service/NovelService.java` | 26, 106–134 | `findByHot(NovelHot, Pageable)`, `adminSearch()` with hot filter |
| `service/PaginationService.java` | 55–56 | `AdminNovelHotPagination(...)` |

### 2.5 Java — Controller (Admin)

| File | Lines | Usage |
|---|---|---|
| `controller/admin/DashboardController.java` | 11, 33–34 | `countByHot(NovelHot.HOT)` → `hotNovelCount` model |
| `controller/admin/NovelController.java` | 19, 54–68 | `hot` request param, admin search filter by `hot` |

### 2.6 NovelService `updateNovel`

```java
// NovelService.java line 86
novelInDataBase.setHot(novel.getHot());   // persists admin-selected hot value
```

---

## 3. Decision: Option A — Remove Manual Hot from Client UI, Keep for Admin Stats

**Analysis of the codebase:**
- The `hot` field is used in **admin search/filter** (`/admin/novel?hot=HOT`) → useful for admin
- The `hotNovelCount` on the dashboard is a useful admin stat
- The admin can manually promote a novel to HOT regardless of read count → keep as **admin-only override**
- The **client-facing** badge must use only `totalReadCount` for consistency

**Chosen strategy: Hybrid — keep admin functionality, remove old client badges**

| Area | Action |
|---|---|
| Client templates (homepage, category, novel detail) | **Remove** old `novel.hot == 'HOT'` badge → use only new `totalReadCount` badge |
| Admin novel list table | **Keep** Hot column (admin info only, not displayed to users) |
| Admin create/update form | **Keep** Hot dropdown (admin override for sorting/filtering) |
| Admin dashboard `hotNovelCount` | **Keep** but update to count by `totalReadCount >= threshold` instead of `hot` enum |
| `NovelService.updateNovel` | **Keep** — admin can still save hot field |
| `ReadCountService` | **Does NOT sync** `hot` enum automatically (keep them independent) |

> **Result:** `novel.hot` becomes an admin-only field.
> Users see Hot badge only when `totalReadCount >= threshold`.
> Admin can still filter/view novels by the manual `hot` field.

---

## 4. Files to Modify

### 4.1 Client Templates — Remove Old Hot Badge

#### `templates/client/homepage/show.html` — Lines 67, 97, 127

Three sections (ORIGINAL, TRANSLATION, COMPLETED) each have:

```diff
 <div class="hotnovel">
-    <span th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"
-          class="badge bg-danger">HOT</span>
+    <!-- Hot badge: auto-calculated from read count only -->
+    <span th:if="${novel.totalReadCount != null and novel.totalReadCount >= hotThreshold}"
+          class="badge bg-danger">HOT</span>
 </div>
```

> **Note:** `hotThreshold` must be added to model by `HomepageController`.

#### `templates/client/category/show.html` — Line 129

```diff
 <div class="hotnovel" style="position: absolute; top: 8px; right: 8px;">
-    <span th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"
-          class="badge bg-danger">HOT</span>
+    <span th:if="${novel.totalReadCount != null and novel.totalReadCount >= hotThreshold}"
+          class="badge bg-danger">HOT</span>
 </div>
```

> **Note:** `hotThreshold` must be added to model by `ClientCategoryController`/`ClientSearchController`.

#### `templates/client/novel/show.html` — (from read-count doc)

No old badge found in this file (it already uses `totalReadCount` from the new system).
Verify line 64–65: the genre badges are correct and not conflicting with Hot.

---

### 4.2 Admin Templates — Keep But Do Not Duplicate for Users

#### `templates/admin/novel/show.html` — Hot column (keep as-is, admin only)

No change. Line 86 shows `novel.hot.displayName` in the admin table — this is for admin reference.

```html
<!-- KEEP — this is admin-only, not visible to end users -->
<td th:text="${novel.hot.displayName}"></td>
```

#### `templates/admin/novel/create.html` — Hot dropdown (keep + add note)

Change the label to clarify this is an **admin override** field:

```diff
 <!-- Hot -->
 <div class="col-md-6 form-group select-role">
-    <label>Hot</label>
+    <label>Hot (admin override)
+        <small class="text-muted d-block fw-normal">
+            Novels are auto-tagged Hot when reads ≥ threshold. This overrides the admin list filter only.
+        </small>
+    </label>
     <div class="select-wrapper">
         <select class="form-control" th:field="*{hot}">
             ...
         </select>
     </div>
 </div>
```

#### `templates/admin/novel/update.html` — Hot dropdown (same change as create)

Same label update as above.

#### `templates/admin/dashboard/show.html` — Update hotNovelCount source

Change the stat card label to clarify it counts by read-count threshold:

```diff
 <div class="ms-3">
-    <h4>Hot Novels</h4>
+    <h4>Hot Novels <small class="text-muted fs-6">(reads ≥ threshold)</small></h4>
     <h3 th:text="${hotNovelCount}">0</h3>
 </div>
```

---

### 4.3 Controller — Add `hotThreshold` to Models

#### `controller/client/HomepageController.java` (or equivalent)

```java
@Value("${hot.novel.read-threshold:1000}")
private long hotReadThreshold;

// In model population:
model.addAttribute("hotThreshold", hotReadThreshold);
```

#### `controller/client/ClientCategoryController.java` (or equivalent)

```java
@Value("${hot.novel.read-threshold:1000}")
private long hotReadThreshold;

model.addAttribute("hotThreshold", hotReadThreshold);
```

#### `controller/client/ReaderController.java`

```java
// already needed from read-count spec — ensure this is added:
model.addAttribute("hotThreshold", hotReadThreshold);
```

#### `controller/admin/DashboardController.java` — Update hotNovelCount

**Option B (recommended):** Count by `totalReadCount >= threshold` instead of enum:

```java
// Before:
model.addAttribute("hotNovelCount",
        this.novelRepository.countByHot(NovelHot.HOT));

// After:
@Value("${hot.novel.read-threshold:1000}")
private long hotReadThreshold;

model.addAttribute("hotNovelCount",
        this.novelRepository.countByTotalReadCountGreaterThanEqual(hotReadThreshold));
```

If you want to keep both counts (manual hot + auto hot), keep both attributes
separately and show them in two dashboard stat cards:

```java
model.addAttribute("hotNovelCount",   // auto: by read count
        this.novelRepository.countByTotalReadCountGreaterThanEqual(hotReadThreshold));
model.addAttribute("adminHotCount",   // manual: by admin enum
        this.novelRepository.countByHot(NovelHot.HOT));
```

> If keeping only one stat, use the **auto (read-count)** version.
> Remove the manual-hot card entirely if desired.

---

### 4.4 Repository — Add New Count Query

#### `NovelRepository.java` — add:

```java
// Count novels with totalReadCount >= threshold (for dashboard)
long countByTotalReadCountGreaterThanEqual(Long threshold);
```

---

### 4.5 `NovelService.java` — Add `isHot()` Helper (Single Source of Truth)

Add a helper so no template/controller duplicates the threshold comparison:

```java
@Value("${hot.novel.read-threshold:1000}")
private long hotReadThreshold;

/**
 * Single source of truth for Hot badge calculation.
 * Use this in controllers/services instead of comparing in templates.
 */
public boolean isHot(Novel novel) {
    long count = novel.getTotalReadCount() == null ? 0L : novel.getTotalReadCount();
    return count >= hotReadThreshold;
}

public long getHotThreshold() {
    return hotReadThreshold;
}
```

> Controllers can then do:
> ```java
> model.addAttribute("hotThreshold", novelService.getHotThreshold());
> ```
> ...so the threshold value is managed in one place.

---

## 5. Complete Before/After — Every Affected Template

### homepage/show.html — ORIGINAL section (apply same to TRANSLATION + COMPLETED)

**Before:**
```html
<div class="hotnovel">
    <span th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"
          class="badge bg-danger">HOT</span>
</div>
<div class="overlay">
    <h6 class="mb-1 fw-bold text-white text-truncate" th:text="${novel.title}"></h6>
    <small class="text-light" th:text="'Tác giả: ' + ${novel.author}"></small>
</div>
```

**After:**
```html
<div class="hotnovel">
    <span th:if="${novel.totalReadCount != null and novel.totalReadCount >= hotThreshold}"
          class="badge bg-danger">HOT</span>
</div>
<div class="overlay">
    <h6 class="mb-1 fw-bold text-white text-truncate" th:text="${novel.title}"></h6>
    <small class="text-light" th:text="'Author: ' + ${novel.author}"></small>
</div>
```

### category/show.html

**Before:**
```html
<div class="hotnovel" style="position: absolute; top: 8px; right: 8px;">
    <span th:if="${novel.hot != null and novel.hot.name() == 'HOT'}"
          class="badge bg-danger">HOT</span>
</div>
```

**After:**
```html
<div class="hotnovel" style="position: absolute; top: 8px; right: 8px;">
    <span th:if="${novel.totalReadCount != null and novel.totalReadCount >= hotThreshold}"
          class="badge bg-danger">HOT</span>
</div>
```

---

## 6. No Database Migration Needed

| Field | Action |
|---|---|
| `Novels.hot` (enum column) | **Keep as-is** — admin data, not deleted |
| `Novels.total_read_count` | Already added by `V3__add_read_count.sql` |
| `NovelHot` enum | **Keep as-is** — still used for admin filter |

No data migration required. The old `hot` values in the DB are preserved
and still used by admin search/filter. They just stop appearing in client UI.

---

## 7. CSS

No CSS changes needed.

The badge uses the same class `badge bg-danger` in both old and new templates.
The appearance is identical — only the **condition** (old enum vs new read count) changes.

If you added a separate class like `novel-badge-hot` in the new system,
ensure only one badge renders. With this fix, only one `<span>` renders per novel card.

---

## 8. Configuration

All in `application.properties`:

```properties
# Hot badge threshold — auto-calculated
hot.novel.read-threshold=1000

# Cooldown (from read-count spec)
read.count.cooldown-minutes=30
```

To change the threshold: update this value and restart. No template change needed.

---

## 9. Edge Cases

| Case | Behavior after fix |
|---|---|
| `novel.hot = HOT` but `totalReadCount < threshold` | No badge shown in client UI |
| `novel.hot = NOT_HOT` but `totalReadCount >= threshold` | Badge shown in client UI ✓ |
| `novel.hot = HOT` AND `totalReadCount >= threshold` | **Single badge** shown ✓ (old badge removed) |
| `totalReadCount` is `null` | Thymeleaf `novel.totalReadCount != null and ...` guards safely → no badge |
| Admin sets `hot = HOT` manually | Only affects admin list filter; no badge shown unless reads ≥ threshold |
| Novel's `totalReadCount` drops below threshold (manual reset) | Badge disappears automatically |

---

## 10. Test Cases

```
1. Novel: hot=NOT_HOT, totalReadCount=500, threshold=1000
   → No HOT badge shown on homepage ✓

2. Novel: hot=NOT_HOT, totalReadCount=1500, threshold=1000
   → Single HOT badge shown on homepage ✓

3. Novel: hot=HOT (manual), totalReadCount=500, threshold=1000
   → No HOT badge shown on homepage (old badge removed) ✓

4. Novel: hot=HOT (manual), totalReadCount=1500, threshold=1000
   → SINGLE HOT badge shown (not two) ✓

5. Novel: hot=NOT_HOT, totalReadCount=null (legacy data)
   → No badge, no NullPointerException ✓

6. Admin form saves hot=HOT
   → Admin filter works; no duplicate badge appears to users ✓

7. Admin dashboard hotNovelCount
   → Shows count of novels with totalReadCount >= threshold ✓

8. Category/search page
   → Only one badge per novel card ✓
```

---

## 11. Summary of All Files Changed

### Modified (client templates)

| File | Change |
|---|---|
| `templates/client/homepage/show.html` | Remove 3× old `novel.hot == 'HOT'` badges → replace with `totalReadCount` badge |
| `templates/client/category/show.html` | Remove 1× old badge → replace with `totalReadCount` badge |

### Modified (admin templates)

| File | Change |
|---|---|
| `templates/admin/novel/create.html` | Update Hot dropdown label to say "admin override" |
| `templates/admin/novel/update.html` | Update Hot dropdown label to say "admin override" |
| `templates/admin/dashboard/show.html` | Update stat card label to clarify it counts by read-count |

### Modified (Java)

| File | Change |
|---|---|
| `service/NovelService.java` | Add `isHot(Novel)` helper + `getHotThreshold()` |
| `repository/NovelRepository.java` | Add `countByTotalReadCountGreaterThanEqual(Long)` |
| `controller/admin/DashboardController.java` | Use `countByTotalReadCountGreaterThanEqual` instead of `countByHot(HOT)` |
| `controller/client/HomepageController.java` | Add `hotThreshold` to model |
| `controller/client/ClientCategoryController.java` | Add `hotThreshold` to model |
| `controller/client/ReaderController.java` | Add `hotThreshold` to model (already from read-count spec) |

### NOT changed

| File | Reason |
|---|---|
| `domain/Novel.java` | Keep `hot` field for admin purposes |
| `domain/NovelHot.java` | Keep enum for admin filter |
| `repository/NovelRepository.java` old queries | Keep `findByHot`, `countByHot` for admin search |
| `service/NovelService.java` `adminSearch()` | Keep admin hot filter |
| `controller/admin/NovelController.java` | Keep hot query param for admin list |
| `templates/admin/novel/show.html` | Keep Hot column for admin reference |
| All bookmark, chapter, auth logic | Unrelated — not touched |
