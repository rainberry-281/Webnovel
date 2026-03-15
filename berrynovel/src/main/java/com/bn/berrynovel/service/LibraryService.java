package com.bn.berrynovel.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bn.berrynovel.domain.Bookshelf;
import com.bn.berrynovel.domain.BookshelfId;
import com.bn.berrynovel.domain.Bookmark;
import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.domain.dto.BookmarkNovelDTO;
import com.bn.berrynovel.domain.dto.BookshelfItemDTO;
import com.bn.berrynovel.repository.BookmarkRepository;
import com.bn.berrynovel.repository.BookshelfRepository;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.repository.NovelRepository;
import com.bn.berrynovel.repository.UserRepository;

@Service
public class LibraryService {
    private final BookshelfRepository bookshelfRepository;
    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final BookmarkRepository bookmarkRepository;

    public LibraryService(BookshelfRepository bookshelfRepository, UserRepository userRepository,
            NovelRepository novelRepository, ChapterRepository chapterRepository,
            BookmarkRepository bookmarkRepository) {
        this.bookshelfRepository = bookshelfRepository;
        this.userRepository = userRepository;
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    public void addNovelToLibrary(String username, Long novelId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (this.bookshelfRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId)) {
            return;
        }

        Novel novel = this.novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setId(new BookshelfId(user.getId(), novel.getId()));
        bookshelf.setUser(user);
        bookshelf.setNovel(novel);

        this.bookshelfRepository.save(bookshelf);
    }

    @Transactional
    public void toggleNovelInLibrary(String username, Long novelId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        boolean inLibrary = this.bookshelfRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId);
        if (inLibrary) {
            this.bookshelfRepository.deleteByUser_IdAndNovel_Id(user.getId(), novelId);
            return;
        }

        this.addNovelToLibrary(username, novelId);
    }

    public boolean isNovelInLibrary(String username, Long novelId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        return this.bookshelfRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId);
    }

    public List<BookshelfItemDTO> getBookshelfItems(String username) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return List.of();
        }

        return this.bookshelfRepository.findByUser_IdOrderByNovel_TitleAsc(user.getId()).stream()
                .map(bookshelf -> {
                    Chapter newestChapter = this.chapterRepository.findLastChapter(bookshelf.getNovel().getId());
                    LocalDateTime latestChapterTime = newestChapter != null && newestChapter.getCreatedAt() != null
                            ? newestChapter.getCreatedAt()
                            : LocalDateTime.MIN;
                    LocalDateTime savedAtTime = bookshelf.getSavedAt() != null
                            ? bookshelf.getSavedAt()
                            : LocalDateTime.MIN;
                    return new BookshelfSortItem(new BookshelfItemDTO(bookshelf.getNovel(), newestChapter),
                            latestChapterTime, savedAtTime);
                })
                .sorted(Comparator
                        .comparing(BookshelfSortItem::latestChapterTime, Comparator.reverseOrder())
                        .thenComparing(BookshelfSortItem::savedAtTime, Comparator.reverseOrder()))
                .map(BookshelfSortItem::item)
                .toList();
    }

    private record BookshelfSortItem(BookshelfItemDTO item, LocalDateTime latestChapterTime,
            LocalDateTime savedAtTime) {
    }

    @Transactional
    public int deleteNovelsFromLibrary(String username, List<Long> novelIds) {
        if (novelIds == null || novelIds.isEmpty()) {
            return 0;
        }

        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return 0;
        }

        List<Bookshelf> matchedItems = this.bookshelfRepository.findByUser_IdOrderByNovel_TitleAsc(user.getId())
                .stream()
                .filter(item -> item.getNovel() != null && item.getNovel().getId() != null
                        && novelIds.contains(item.getNovel().getId()))
                .toList();

        this.bookshelfRepository.deleteByUser_IdAndNovel_IdIn(user.getId(), novelIds);
        return matchedItems.size();
    }

    @Transactional
    public void toggleChapterBookmark(String username, Long novelId, Long chapterId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Chapter chapter = this.chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        if (chapter.getNovel() == null || chapter.getNovel().getId() == null
                || !chapter.getNovel().getId().equals(novelId)) {
            throw new RuntimeException("Chapter does not belong to novel");
        }

        boolean exists = this.bookmarkRepository.existsByUser_IdAndChapter_Id(user.getId(), chapterId);
        if (exists) {
            this.bookmarkRepository.deleteByUser_IdAndChapter_Id(user.getId(), chapterId);
            return;
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setNovel(chapter.getNovel());
        bookmark.setChapter(chapter);
        this.bookmarkRepository.save(bookmark);
    }

    public boolean isChapterBookmarked(String username, Long chapterId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        return this.bookmarkRepository.existsByUser_IdAndChapter_Id(user.getId(), chapterId);
    }

    public List<BookmarkNovelDTO> getBookmarkItems(String username) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return List.of();
        }

        java.util.LinkedHashMap<Long, java.util.List<Chapter>> groupedChapters = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Long, Novel> novelsById = new java.util.LinkedHashMap<>();

        this.bookmarkRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .forEach(bookmark -> {
                    Long id = bookmark.getNovel().getId();
                    novelsById.putIfAbsent(id, bookmark.getNovel());
                    groupedChapters.computeIfAbsent(id, key -> new java.util.ArrayList<>())
                            .add(bookmark.getChapter());
                });

        return novelsById.entrySet().stream()
                .map(entry -> new BookmarkNovelDTO(entry.getValue(),
                        groupedChapters.getOrDefault(entry.getKey(), List.of())))
                .toList();
    }
}
