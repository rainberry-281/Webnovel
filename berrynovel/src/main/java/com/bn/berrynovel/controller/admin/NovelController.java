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

import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.PaginationQuery;
import com.bn.berrynovel.domain.Genre;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import com.bn.berrynovel.service.ImageService;
import com.bn.berrynovel.service.PaginationService;

@Controller
@RequestMapping("/admin/novel")
public class NovelController {

    private final NovelService novelService;
    private final GenreRepository genreRepository;
    private final ImageService imageService;
    private final PaginationService paginationService;

    public NovelController(NovelService novelService, GenreRepository genreRepository, ImageService imageService,
            PaginationService paginationService) {
        this.novelService = novelService;
        this.genreRepository = genreRepository;
        this.imageService = imageService;
        this.paginationService = paginationService;
    }

    @GetMapping
    public String getNovelListPage(Model model, @RequestParam(value = "page") Optional<String> pageOptional) {
        PaginationQuery paginationQuery = this.paginationService.AdminNovelPagination(pageOptional, 8);
        List<Novel> novels = this.novelService.getAllNovels();
        System.out.println(">>>>>>> novel list: " + novels);
        model.addAttribute("novels", paginationQuery.getNvs().getContent());

        model.addAttribute("currentPage", paginationQuery.getPage());

        model.addAttribute("totalPage", paginationQuery.getNvs().getTotalPages());

        return "admin/novel/show";
    }

    @GetMapping("/create")
    public String getNovelCreatePage(Model model) {
        model.addAttribute("newNovel", new Novel());
        model.addAttribute("genres", this.novelService.getAllGenres());
        return "admin/novel/create";
    }

    @PostMapping("/create")
    public String postCreateNovel(@ModelAttribute("newNovel") Novel novel,
            @RequestParam(value = "images", required = false) MultipartFile file,
            @RequestParam(value = "genreIds", required = false) List<Integer> genreIds) {
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreIds.stream()
                    .map(genreId -> this.genreRepository.findById(genreId).orElse(null))
                    .filter(g -> g != null)
                    .toList();
            novel.setGenres(genres);
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
    public String updateNovelPage(@PathVariable("id") Long id, @ModelAttribute("newNovel") Novel novel,
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
        this.novelService.updateNovel(novel, file);
        return "redirect:/admin/novel";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleNovelStatus(@PathVariable Long id) {
        this.novelService.toggleNovelStatus(id);
        return "redirect:/admin/novel";
    }
}
