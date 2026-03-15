package com.bn.berrynovel.controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import org.springframework.security.core.Authentication;

import javax.naming.Binding;

import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.PaginationQuery;
import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.domain.User;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Controller;
import com.bn.berrynovel.service.ImageService;
import com.bn.berrynovel.service.PaginationService;
import com.bn.berrynovel.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/novel")
public class NovelController {

    private final NovelService novelService;
    private final GenreRepository genreRepository;
    private final ImageService imageService;
    private final PaginationService paginationService;
    private final UserService userService;

    public NovelController(NovelService novelService, GenreRepository genreRepository, ImageService imageService,
            PaginationService paginationService, UserService userService) {
        this.novelService = novelService;
        this.genreRepository = genreRepository;
        this.imageService = imageService;
        this.paginationService = paginationService;
        this.userService = userService;
    }

    @GetMapping
    public String getNovelListPage(Model model,
            @RequestParam(value = "page") Optional<String> pageOptional,
            @RequestParam(value = "keyword", required = false) String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        PaginationQuery paginationQuery = this.paginationService.AdminNovelPagination(pageOptional, 8,
                normalizedKeyword);
        model.addAttribute("novels", paginationQuery.getNvs().getContent());

        model.addAttribute("currentPage", paginationQuery.getPage());

        model.addAttribute("totalPage", paginationQuery.getNvs().getTotalPages());

        model.addAttribute("keyword", normalizedKeyword);

        return "admin/novel/show";
    }

    @GetMapping("/create")
    public String getNovelCreatePage(Model model) {
        model.addAttribute("newNovel", new Novel());
        model.addAttribute("genres", this.novelService.getAllGenres());
        return "admin/novel/create";
    }

    @PostMapping("/create")
    public String postCreateNovel(@ModelAttribute("newNovel") @Valid Novel novel,
            BindingResult novelBindingResult,
            @RequestParam(value = "images", required = false) MultipartFile file,
            @RequestParam(value = "genreIds", required = false) List<Integer> genreIds,
            @RequestParam(value = "uploaderID", required = false) Long uploaderID,
            Authentication authentication) {
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreIds.stream()
                    .map(genreId -> this.genreRepository.findById(genreId).orElse(null))
                    .filter(g -> g != null)
                    .toList();
            novel.setGenres(genres);
        }

        if (this.novelService.checkTitleExists(novel.getTitle())) {
            novelBindingResult.rejectValue("title", "error.novel", "Title already exists");
        }

        if (novelBindingResult.hasErrors()) {
            return "admin/novel/create";
        }

        User uploader = null;
        if (uploaderID != null) {
            uploader = this.userService.getUserByID(uploaderID);
        }
        if (uploader == null && authentication != null) {
            uploader = this.userService.getUserByUsername(authentication.getName());
        }
        if (uploader != null) {
            novel.setUser(uploader);
        }

        novel.setCreatedAt(LocalDateTime.now());
        this.novelService.createNovel(novel, file);
        return "redirect:/admin/novel";
    }

    @GetMapping("/update/{id}")
    public String getNovelUpdatePage(@PathVariable("id") Long id, Model model) {
        Novel novel = this.novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        model.addAttribute("newNovel", novel);
        model.addAttribute("genres", this.novelService.getAllGenres());
        model.addAttribute("selectedGenreIds", novel.getGenres() == null ? Collections.emptyList()
                : novel.getGenres().stream().map(Genre::getId).toList());
        return "admin/novel/update";
    }

    @PostMapping("/update/{id}")
    public String updateNovelPage(@PathVariable("id") Long id, @ModelAttribute("newNovel") @Valid Novel novel,
            BindingResult novelBindingResult,
            @RequestParam(value = "images", required = false) MultipartFile file,
            @RequestParam(value = "genreIds", required = false) List<Integer> genreIds) {
        novel.setId(id);
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreIds.stream()
                    .map(genreId -> this.genreRepository.findById(genreId).orElse(null))
                    .filter(g -> g != null)
                    .toList();
            novel.setGenres(genres);
        }

        if (this.novelService.checkTitleExists(novel.getTitle())) {
            novelBindingResult.rejectValue("title", "error.novel", "Title already exists");
        }

        if (novelBindingResult.hasErrors()) {
            return "admin/novel/update";
        }
        this.novelService.updateNovel(novel, file);
        return "redirect:/admin/novel";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleNovelStatus(@PathVariable Long id) {
        this.novelService.toggleNovelStatus(id);
        return "redirect:/admin/novel";
    }
}
