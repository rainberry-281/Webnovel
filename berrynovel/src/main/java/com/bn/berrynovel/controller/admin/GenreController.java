package com.bn.berrynovel.controller.admin;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.service.PaginationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

    private String buildGenreListRedirect(Optional<String> pageOptional) {
        if (pageOptional.isPresent() && !pageOptional.get().isBlank()) {
            return "redirect:/admin/genres?page=" + pageOptional.get();
        }
        return "redirect:/admin/genres";
    }

    private void loadGenreListPageData(Model model, Optional<String> pageOptional, Genre genreForm) {

        PaginationQuery paginationQuery = this.paginationService.AdminGenrePagination(pageOptional, 8);
        List<Genre> genres = paginationQuery.getNvs().getContent();

        model.addAttribute("genres", genres);
        model.addAttribute("currentPage", paginationQuery.getPage());
        model.addAttribute("totalPage", paginationQuery.getNvs().getTotalPages());
        model.addAttribute("genre", genreForm);
    }

    @GetMapping()
    public String getGenreList(Model model, @RequestParam(value = "page") Optional<String> pageOptional) {
        loadGenreListPageData(model, pageOptional, new Genre());
        return "admin/genre/show";
    }

    @PostMapping("/create")
    public String createGenre(@ModelAttribute("genre") @Valid Genre genre, BindingResult bindingResult,
            @RequestParam(value = "page", required = false) Optional<String> pageOptional,
            Model model) {

        if (this.novelService.checkGenreNameExists(genre.getName())) {
            bindingResult.rejectValue("name", "error.genre", "Genre name already exists");
        }

        if (genre.getName() == null || genre.getName().trim().isEmpty()) {
            bindingResult.rejectValue("name", "error.genre", "Genre name cannot be empty");
        }

        if (bindingResult.hasErrors()) {
            loadGenreListPageData(model, pageOptional, genre);
            model.addAttribute("openGenrePopup", true);
            model.addAttribute("popupMode", "create");
            return "admin/genre/show";
        }

        this.novelService.saveGenre(genre);
        return buildGenreListRedirect(pageOptional);
    }

    @PostMapping("/update/{id}")
    public String updateGenre(@PathVariable Integer id, @ModelAttribute("genre") @Valid Genre genre,
            BindingResult bindingResult,
            @RequestParam(value = "page", required = false) Optional<String> pageOptional,
            Model model) {
        genre.setId(id);

        if (this.novelService.checkGenreNameExists(genre.getName())) {
            bindingResult.rejectValue("name", "error.genre", "Genre name already exists");
        }

        if (genre.getName() == null || genre.getName().trim().isEmpty()) {
            bindingResult.rejectValue("name", "error.genre", "Genre name cannot be empty");
        }

        if (bindingResult.hasErrors()) {
            loadGenreListPageData(model, pageOptional, genre);
            model.addAttribute("openGenrePopup", true);
            model.addAttribute("popupMode", "update");
            return "admin/genre/show";
        }

        this.novelService.updateGenre(genre);

        return buildGenreListRedirect(pageOptional);

    }

    @PostMapping("/action/{id}")
    public String toggleGenreStatus(@PathVariable Integer id,
            @RequestParam(value = "page", required = false) Optional<String> pageOptional) {

        this.novelService.actionGenre(id);
        return buildGenreListRedirect(pageOptional);
    }
}
