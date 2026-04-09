package com.bn.berrynovel.service;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.function.Function;

import com.bn.berrynovel.domain.PaginationQuery;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.domain.Genre;

@Service
public class PaginationService {
    private final NovelService novelService;
    private final UserService userService;

    public PaginationService(NovelService novelService, UserService userService) {
        this.novelService = novelService;
        this.userService = userService;
    }

    private int resolvePage(Optional<String> pageOptional) {
        if (pageOptional.isEmpty()) {
            return 1;
        }

        try {
            int parsedPage = Integer.parseInt(pageOptional.get());
            return Math.max(parsedPage, 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private <T> PaginationQuery<T> paginate(Optional<String> pageOptional, int size,
            Function<Pageable, Page<T>> fetcher) {
        int page = resolvePage(pageOptional);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<T> data = fetcher.apply(pageable);
        return new PaginationQuery<>(page, data);
    }

    public PaginationQuery<Novel> AdminNovelPagination(Optional<String> pageOptinal, int size) {
        return paginate(pageOptinal, size, this.novelService::findAll);
    }

    public PaginationQuery<Novel> AdminNovelPagination(Optional<String> pageOptinal, int size, String keyword) {
        return paginate(pageOptinal, size, pageable -> this.novelService.findByTitleContaining(keyword, pageable));
    }

    public PaginationQuery<Novel> AdminNovelHotPagination(Optional<String> pageOptinal, int size,
            com.bn.berrynovel.domain.NovelHot hot) {
        return paginate(pageOptinal, size, pageable -> this.novelService.findByHot(hot, pageable));
    }

    public PaginationQuery<Novel> AdminNovelSearch(Optional<String> pageOptinal, int size, String keyword,
            String hot) {
        return paginate(pageOptinal, size, pageable -> this.novelService.adminSearch(keyword, hot, pageable));
    }

    public PaginationQuery<Novel> ClientNovelPagination(Optional<String> pageOptinal, int size) {
        return paginate(pageOptinal, size, this.novelService::findActiveNovelsWithActiveGenres);
    }

    public PaginationQuery<Novel> ClientSearchNovelPagination(Optional<String> pageOptinal, int size, String keyword) {
        return paginate(pageOptinal, size, pageable -> this.novelService.searchVisibleNovelsByTitle(keyword, pageable));
    }

    public PaginationQuery<Novel> ClientFilterNovelPagination(Optional<String> pageOptinal, int size, String keyword,
            java.util.List<Integer> genreIds, java.util.List<String> typeValues,
            java.util.List<String> progressValues) {
        return paginate(pageOptinal, size,
                pageable -> this.novelService.searchVisibleNovels(keyword, genreIds, typeValues, progressValues,
                        pageable));
    }

    public PaginationQuery<User> AdminUserPagination(Optional<String> pageOptinal, int size) {
        return paginate(pageOptinal, size, this.userService::findAll);
    }

    public PaginationQuery<User> AdminUserPagination(Optional<String> pageOptinal, int size, String keyword) {
        return paginate(pageOptinal, size, pageable -> this.userService.findByUsernameContaining(keyword, pageable));
    }

    public PaginationQuery<Genre> AdminGenrePagination(Optional<String> pageOptinal, int size) {
        return paginate(pageOptinal, size, this.novelService::findAllGenres);
    }
}
