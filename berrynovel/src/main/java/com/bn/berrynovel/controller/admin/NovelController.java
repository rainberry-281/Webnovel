package com.bn.berrynovel.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.Genre;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import com.bn.berrynovel.service.ImageService;

@Controller
@RequestMapping("/admin/novel")
public class NovelController {

    private final NovelService novelService;
    private final GenreRepository genreRepository;
    private final ImageService imageService;

    public NovelController(NovelService novelService, GenreRepository genreRepository, ImageService imageService) {
        this.novelService = novelService;
        this.genreRepository = genreRepository;
        this.imageService = imageService;
    }

    @GetMapping
    public String getNovelListPage(Model model) {
        List<Novel> novels = this.novelService.getAllNovels();
        System.out.println(">>>>>>> novel list: " + novels);
        model.addAttribute("novels", novels);
        return "admin/novel/show";
    }

    @GetMapping("/create")
    public String getNovelCreatePage(Model model) {
        model.addAttribute("novel", new Novel());
        model.addAttribute("genres", this.novelService.getAllGenres());
        return "admin/novel/create";
    }

    @PostMapping("/create")
    public String postCreateNovel(@ModelAttribute("newNovel") Novel novel,
            @RequestParam(value = "images", required = false) MultipartFile file,
            @RequestParam(value = "genreIds", required = false) List<Integer> genreIds) {
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreIds.stream()
                    .map(id -> this.genreRepository.findById(id).orElse(null))
                    .filter(g -> g != null)
                    .toList();
            novel.setGenres(genres);
        }
        this.novelService.updateNovel(novel, file);
        return "redirect:/admin/novel";
    }

    @GetMapping("/update/{id}")
    public String getNovelUpdatePage(@PathVariable("id") Long id, Model model) {
        Novel novel = this.novelService.getNovelById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        model.addAttribute("newNovel", novel);
        model.addAttribute("genres", this.novelService.getAllGenres());
        return "admin/novel/update";
    }

    @PostMapping("/update")
    public String updateNovelPage(@ModelAttribute("newNovel") Novel novel,
            @RequestParam(value = "images", required = false) MultipartFile file,
            @RequestParam(value = "genreIds", required = false) List<Integer> genreIds) {
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreIds.stream()
                    .map(id -> this.genreRepository.findById(id).orElse(null))
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
