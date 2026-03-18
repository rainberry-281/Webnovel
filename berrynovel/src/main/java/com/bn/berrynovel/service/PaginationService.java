package com.bn.berrynovel.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import com.bn.berrynovel.domain.PaginationQuery;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.repository.NovelRepository;
import com.bn.berrynovel.repository.UserRepository;

@Service
public class PaginationService {
    private final NovelService novelService;
    private final UserService userService;

    public PaginationService(NovelService novelService, UserService userService) {
        this.novelService = novelService;
        this.userService = userService;
    }

    public PaginationQuery<Novel> AdminNovelPagination(Optional<String> pageOptinal, int size) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Novel> nvs = this.novelService.findAll(pageable);

        return new PaginationQuery<>(page, nvs);
    }

    public PaginationQuery<Novel> AdminNovelPagination(Optional<String> pageOptinal, int size, String keyword) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Novel> nvs = this.novelService.findByTitleContaining(keyword, pageable);

        return new PaginationQuery<>(page, nvs);
    }

    public PaginationQuery<Novel> ClientNovelPagination(Optional<String> pageOptinal, int size) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Novel> nvs = this.novelService.findActiveNovelsWithActiveGenres(pageable);

        return new PaginationQuery<>(page, nvs);
    }

    public PaginationQuery<Novel> ClientSearchNovelPagination(Optional<String> pageOptinal, int size, String keyword) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Novel> nvs = this.novelService.searchVisibleNovelsByTitle(keyword, pageable);

        return new PaginationQuery<>(page, nvs);
    }

    public PaginationQuery<Novel> ClientFilterNovelPagination(Optional<String> pageOptinal, int size, String keyword,
            java.util.List<Integer> genreIds, java.util.List<String> typeValues,
            java.util.List<String> progressValues) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Novel> nvs = this.novelService.searchVisibleNovels(keyword, genreIds, typeValues, progressValues,
                pageable);

        return new PaginationQuery<>(page, nvs);
    }

    public PaginationQuery<User> AdminUserPagination(Optional<String> pageOptinal, int size) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<User> users = this.userService.findAll(pageable);

        return new PaginationQuery<>(page, users);
    }

    public PaginationQuery<User> AdminUserPagination(Optional<String> pageOptinal, int size, String keyword) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<User> users = this.userService.findByUsernameContaining(keyword, pageable);

        return new PaginationQuery<>(page, users);
    }

    public PaginationQuery<Genre> AdminGenrePagination(Optional<String> pageOptinal, int size) {
        int page = 1;
        try {
            if (pageOptinal.isPresent()) {
                page = Integer.parseInt(pageOptinal.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Genre> genres = this.novelService.findAllGenres(pageable);

        return new PaginationQuery<>(page, genres);
    }
}
