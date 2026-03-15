package com.bn.berrynovel.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bn.berrynovel.domain.Bookshelf;
import com.bn.berrynovel.domain.BookshelfId;
import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.domain.dto.BookshelfItemDTO;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.repository.LibraryRepository;
import com.bn.berrynovel.repository.NovelRepository;
import com.bn.berrynovel.repository.UserRepository;

@Service
public class LibraryService {
    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public LibraryService(LibraryRepository libraryRepository, UserRepository userRepository,
            NovelRepository novelRepository, ChapterRepository chapterRepository) {
        this.libraryRepository = libraryRepository;
        this.userRepository = userRepository;
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    public void addNovelToLibrary(String username, Long novelId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (this.libraryRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId)) {
            return;
        }

        Novel novel = this.novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setId(new BookshelfId(user.getId(), novel.getId()));
        bookshelf.setUser(user);
        bookshelf.setNovel(novel);

        this.libraryRepository.save(bookshelf);
    }

    @Transactional
    public void toggleNovelInLibrary(String username, Long novelId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        boolean inLibrary = this.libraryRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId);
        if (inLibrary) {
            this.libraryRepository.deleteByUser_IdAndNovel_Id(user.getId(), novelId);
            return;
        }

        this.addNovelToLibrary(username, novelId);
    }

    public boolean isNovelInLibrary(String username, Long novelId) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        return this.libraryRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId);
    }

    public List<BookshelfItemDTO> getBookshelfItems(String username) {
        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return List.of();
        }

        return this.libraryRepository.findByUser_IdOrderByNovel_TitleAsc(user.getId()).stream()
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
    public void deleteNovelsFromLibrary(String username, List<Long> novelIds) {
        if (novelIds == null || novelIds.isEmpty()) {
            return;
        }

        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            return;
        }

        this.libraryRepository.deleteByUser_IdAndNovel_IdIn(user.getId(), novelIds);
    }
}
