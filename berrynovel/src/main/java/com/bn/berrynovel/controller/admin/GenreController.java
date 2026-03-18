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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.domain.PaginationQuery;

@Controller
@RequestMapping("/admin/genres")
public class GenreController {
    private static final Logger logger = LoggerFactory.getLogger(GenreController.class);
    private static final String LOG_DIVIDER = "============================================================";

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

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        for (int i = 0; i < genres.size(); i++) {
            Genre genre = genres.get(i);
            sb.append(">>>>>>>>>>> Genre[").append(i + 1).append("]\n")
                    .append("id=").append(genre.getId()).append("\n")
                    .append("name=").append(genre.getName()).append("\n")
                    .append("status=").append(genre.getStatus());
            if (i < genres.size() - 1) {
                sb.append("\n\n");
            }
        }
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        logger.info(sb.toString());

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

        logger.info("\n{}\n>>>>>>>>>>> [CREATE GENRE - REQUEST]\nname={}\nstatus={}\n{}\n",
                LOG_DIVIDER,
                genre.getName(),
                genre.getStatus(),
                LOG_DIVIDER);

        this.novelService.saveGenre(genre);

        logger.info("\n{}\n>>>>>>>>>>> [CREATE GENRE - SUCCESS] name={} created successfully\n{}\n",
                LOG_DIVIDER,
                genre.getName(),
                LOG_DIVIDER);
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

        logger.info("\n{}\n>>>>>>>>>>> [UPDATE GENRE - REQUEST]\nid={}\nname={}\nstatus={}\n{}\n",
                LOG_DIVIDER,
                genre.getId(),
                genre.getName(),
                genre.getStatus(),
                LOG_DIVIDER);

        this.novelService.updateGenre(genre);

        logger.info("\n{}\n>>>>>>>>>>> [UPDATE GENRE - SUCCESS] id={}, name={} updated successfully\n{}\n",
                LOG_DIVIDER,
                genre.getId(),
                genre.getName(),
                LOG_DIVIDER);

        return buildGenreListRedirect(pageOptional);

    }

    @PostMapping("/action/{id}")
    public String toggleGenreStatus(@PathVariable Integer id,
            @RequestParam(value = "page", required = false) Optional<String> pageOptional) {

        Optional<Genre> beforeGenreOptional = this.novelService.getGenreById(id);
        logger.info("\n{}\n>>>>>>>>>>> [TOGGLE GENRE STATUS - REQUEST]\nid={}\nname={}\nstatusBefore={}\n{}\n",
                LOG_DIVIDER,
                id,
                beforeGenreOptional.map(Genre::getName).orElse("N/A"),
                beforeGenreOptional.map(Genre::getStatus).orElse(null),
                LOG_DIVIDER);

        this.novelService.actionGenre(id);

        Optional<Genre> afterGenreOptional = this.novelService.getGenreById(id);
        logger.info("\n{}\n>>>>>>>>>>> [TOGGLE GENRE STATUS - SUCCESS]\nid={}\nname={}\nstatusAfter={}\n{}\n",
                LOG_DIVIDER,
                id,
                afterGenreOptional.map(Genre::getName).orElse("N/A"),
                afterGenreOptional.map(Genre::getStatus).orElse(null),
                LOG_DIVIDER);
        return buildGenreListRedirect(pageOptional);
    }
}
