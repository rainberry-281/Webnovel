package com.bn.berrynovel.controller.admin;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.service.PaginationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.domain.PaginationQuery;

@Controller
@RequestMapping("/admin/genres")
public class GenreController {
    private final NovelService novelService;
    private final PaginationService paginationService;

    public GenreController(NovelService novelService, PaginationService paginationService) {
        this.novelService = novelService;
        this.paginationService = paginationService;
    }

    @GetMapping()
    public String getGenreList(Model model, @RequestParam(value = "page") Optional<String> pageOptional) {
        PaginationQuery paginationQuery = this.paginationService.AdminGenrePagination(pageOptional, 8);
        model.addAttribute("genres", paginationQuery.getNvs().getContent());
        model.addAttribute("currentPage", paginationQuery.getPage());
        model.addAttribute("totalPage", paginationQuery.getNvs().getTotalPages());
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
