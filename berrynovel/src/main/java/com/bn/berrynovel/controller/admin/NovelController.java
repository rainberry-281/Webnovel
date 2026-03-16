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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Binding;

import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.PaginationQuery;
import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.domain.User;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.stereotype.Controller;
import com.bn.berrynovel.service.ImageService;
import com.bn.berrynovel.service.PaginationService;
import com.bn.berrynovel.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/novel")
public class NovelController {

    private static final Logger logger = LoggerFactory.getLogger(NovelController.class);
    private static final String LOG_DIVIDER = "============================================================";

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
        List<Novel> novels = paginationQuery.getNvs().getContent();
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        for (int i = 0; i < novels.size(); i++) {
            Novel n = novels.get(i);
            sb.append(">>>>>>>>>>> Novel[").append(i + 1).append("]\n")
                    .append("id=").append(n.getId()).append("\n")
                    .append("title=").append(n.getTitle()).append("\n")
                    .append("author=").append(n.getAuthor()).append("\n")
                    .append("image=").append(n.getImage()).append("\n")
                    .append("status=").append(n.getStatus()).append("\n")
                    .append("createdAt=").append(n.getCreatedAt());
            if (i < novels.size() - 1)
                sb.append("\n\n");
        }
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        logger.info(sb.toString());
        model.addAttribute("novels", novels);

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

        List<FieldError> errors = novelBindingResult.getFieldErrors();
        for (FieldError error : errors) {
            System.out.println("\n\n>>>>>" + error.getObjectName() + " - " + error.getDefaultMessage() + "\n\n");
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
        logger.info(
                "\n{}\n>>>>>>>>>>> [CREATE NOVEL - REQUEST]\n"
                + "title={}\nauthor={}\nimage={}\ntype={}\nprogress={}\nstatus={}\ngenres={}\nuploaderId={}\ncreatedAt={}\n{}\n",
                LOG_DIVIDER,
                novel.getTitle(),
                novel.getAuthor(),
            novel.getImage(),
                novel.getType(),
                novel.getProgress(),
                novel.getStatus(),
                novel.getGenres() == null ? List.of() : novel.getGenres().stream().map(Genre::getName).toList(),
                novel.getUser() == null ? null : novel.getUser().getId(),
                novel.getCreatedAt(),
                LOG_DIVIDER);
        this.novelService.createNovel(novel, file);
        logger.info("\n{}\n>>>>>>>>>>> [CREATE NOVEL - SUCCESS] title={}, image={} created successfully\n{}\n",
                LOG_DIVIDER,
                novel.getTitle(),
            novel.getImage(),
                LOG_DIVIDER);
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
        logger.info(
                "\n{}\n>>>>>>>>>>> [UPDATE NOVEL - REQUEST]\n"
                + "id={}\ntitle={}\nauthor={}\ntype={}\nprogress={}\nstatus={}\ngenres={}\n{}\n",
                LOG_DIVIDER,
                novel.getId(),
                novel.getTitle(),
                novel.getAuthor(),
                novel.getType(),
                novel.getProgress(),
                novel.getStatus(),
                novel.getGenres() == null ? List.of() : novel.getGenres().stream().map(Genre::getName).toList(),
                LOG_DIVIDER);
        this.novelService.updateNovel(novel, file);
        logger.info("\n{}\n>>>>>>>>>>> [UPDATE NOVEL - SUCCESS] id={}, title={} updated successfully\n{}\n",
                LOG_DIVIDER,
                novel.getId(),
                novel.getTitle(),
                LOG_DIVIDER);
        return "redirect:/admin/novel";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleNovelStatus(@PathVariable Long id) {
        Optional<Novel> beforeNovelOptional = this.novelService.getNovelById(id);
        logger.info("\n{}\n>>>>>>>>>>> [TOGGLE STATUS - REQUEST]\nid={}\ntitle={}\nimage={}\nstatusBefore={}\n{}\n",
            LOG_DIVIDER,
            id,
            beforeNovelOptional.map(Novel::getTitle).orElse("N/A"),
            beforeNovelOptional.map(Novel::getImage).orElse(null),
            beforeNovelOptional.map(Novel::getStatus).orElse(null),
            LOG_DIVIDER);
        this.novelService.toggleNovelStatus(id);
        Optional<Novel> afterNovelOptional = this.novelService.getNovelById(id);
        logger.info("\n{}\n>>>>>>>>>>> [TOGGLE STATUS - SUCCESS]\nid={}\ntitle={}\nimage={}\nstatusAfter={}\n{}\n",
            LOG_DIVIDER,
            id,
            afterNovelOptional.map(Novel::getTitle).orElse("N/A"),
            afterNovelOptional.map(Novel::getImage).orElse(null),
            afterNovelOptional.map(Novel::getStatus).orElse(null),
            LOG_DIVIDER);
        return "redirect:/admin/novel";
    }
}
