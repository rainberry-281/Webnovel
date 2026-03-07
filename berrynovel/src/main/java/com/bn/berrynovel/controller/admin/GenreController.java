package com.bn.berrynovel.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bn.berrynovel.service.NovelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.bn.berrynovel.domain.Genre;

@Controller
@RequestMapping("/admin/genres")
public class GenreController {
    private final NovelService novelService;

    public GenreController(NovelService novelService) {
        this.novelService = novelService;
    }

    @GetMapping()
    public String getGenreList(Model model) {
        model.addAttribute("genres", this.novelService.getAllGenres());
        model.addAttribute("genre", new Genre());
        return "admin/genre/show";
    }

    @PostMapping("/genre/create")
    public String createGenre(@ModelAttribute("genre") Genre genre) {
        this.novelService.saveGenre(genre);
        return "redirect:/admin/genres";
    }

    @PostMapping("/genre/update/{id}")
    public String updateGenre(@PathVariable Integer id, @ModelAttribute("genre") Genre genre) {
        this.novelService.updateGenre(genre);
        return "redirect:/admin/genres";
    }

    @PostMapping("/genre/action/{id}")
    public String toggleGenreStatus(@PathVariable Integer id) {
        this.novelService.actionGenre(id);
        return "redirect:/admin/genres";
    }
}
