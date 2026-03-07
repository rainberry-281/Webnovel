package com.bn.berrynovel.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.domain.Novel;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import com.bn.berrynovel.service.ImageService;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String postCreateNovel(@ModelAttribute("novel") Novel novel,
            @RequestParam(value = "images", required = false) MultipartFile file) {
        this.novelService.saveNovel(novel, file);
        return "redirect:/admin/novel";
    }

}
