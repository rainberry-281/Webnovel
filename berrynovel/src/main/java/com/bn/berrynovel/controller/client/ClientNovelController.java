package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bn.berrynovel.service.LibraryService;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.service.PaginationService;
import com.bn.berrynovel.domain.PaginationQuery;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.Chapter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class ClientNovelController {
    private final NovelService novelService;
    private final PaginationService paginationService;
    private final LibraryService libraryService;

    public ClientNovelController(NovelService novelService, PaginationService paginationService,
            LibraryService libraryService) {
        this.novelService = novelService;
        this.paginationService = paginationService;
        this.libraryService = libraryService;
    }

    @GetMapping("/category")
    public String getCategoryPage(Model model,
            @RequestParam("page") Optional<String> pageOptional,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "genres", required = false) List<Integer> genres,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "progresses", required = false) List<String> progresses) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Integer> selectedGenres = genres == null
                ? List.of()
                : genres.stream().filter(g -> g != null && g > 0).distinct().toList();
        List<String> selectedTypes = types == null
                ? List.of()
                : types.stream().filter(t -> t != null && !t.trim().isEmpty()).map(String::trim).distinct().toList();
        List<String> selectedProgresses = progresses == null
                ? List.of()
                : progresses.stream().filter(p -> p != null && !p.trim().isEmpty()).map(String::trim).distinct()
                        .toList();

        PaginationQuery<Novel> nvs;
        if (normalizedKeyword.isEmpty() && selectedGenres.isEmpty() && selectedTypes.isEmpty()
                && selectedProgresses.isEmpty()) {
            nvs = this.paginationService.ClientNovelPagination(pageOptional, 20);
        } else {
            nvs = this.paginationService.ClientFilterNovelPagination(pageOptional, 20, normalizedKeyword,
                    selectedGenres, selectedTypes, selectedProgresses);
        }

        model.addAttribute("novels", nvs.getNvs().getContent());
        model.addAttribute("currentPage", nvs.getPage());
        model.addAttribute("totalPage", nvs.getNvs().getTotalPages());
        model.addAttribute("genres", this.novelService.getAllGenres());
        model.addAttribute("selectedGenres", selectedGenres);
        model.addAttribute("selectedTypes", selectedTypes);
        model.addAttribute("selectedProgresses", selectedProgresses);
        model.addAttribute("keyword", normalizedKeyword);
        boolean hasFilters = !normalizedKeyword.isEmpty()
                || !selectedGenres.isEmpty()
                || !selectedTypes.isEmpty()
                || !selectedProgresses.isEmpty();
        model.addAttribute("hasFilters", hasFilters);
        return "client/category/show";
    }

    @GetMapping("/search")
    public String searchNovels(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam("page") Optional<String> pageOptional,
            @RequestParam(value = "genres", required = false) List<Integer> genres,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "progresses", required = false) List<String> progresses,
            Model model) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Integer> selectedGenres = genres == null
                ? List.of()
                : genres.stream().filter(g -> g != null && g > 0).distinct().toList();
        List<String> selectedTypes = types == null
                ? List.of()
                : types.stream().filter(t -> t != null && !t.trim().isEmpty()).map(String::trim).distinct().toList();
        List<String> selectedProgresses = progresses == null
                ? List.of()
                : progresses.stream().filter(p -> p != null && !p.trim().isEmpty()).map(String::trim).distinct()
                        .toList();
        if (normalizedKeyword.isEmpty() && selectedGenres.isEmpty() && selectedTypes.isEmpty()
                && selectedProgresses.isEmpty()) {
            return "redirect:/category";
        }

        PaginationQuery<Novel> nvs = this.paginationService.ClientFilterNovelPagination(pageOptional, 20,
                normalizedKeyword, selectedGenres, selectedTypes, selectedProgresses);

        model.addAttribute("novels", nvs.getNvs().getContent());
        model.addAttribute("currentPage", nvs.getPage());
        model.addAttribute("totalPage", nvs.getNvs().getTotalPages());
        model.addAttribute("genres", this.novelService.getAllGenres());
        model.addAttribute("selectedGenres", selectedGenres);
        model.addAttribute("selectedTypes", selectedTypes);
        model.addAttribute("selectedProgresses", selectedProgresses);
        model.addAttribute("keyword", normalizedKeyword);
        boolean hasFilters = !normalizedKeyword.isEmpty()
                || !selectedGenres.isEmpty()
                || !selectedTypes.isEmpty()
                || !selectedProgresses.isEmpty();
        model.addAttribute("hasFilters", hasFilters);
        return "client/category/show";
    }

    @GetMapping("/novel/{id}")
    public String getNovelDetailPage(@PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) Optional<String> from,
            Model model, HttpServletRequest request, Authentication authentication) {
        Novel novel = this.novelService.getNovelById(id).orElseThrow(() -> new RuntimeException("Novel not found"));

        List<Chapter> chapters = this.novelService.getChaptersByNovelId(id);

        Optional<Chapter> firstChapter = this.novelService.getFirstChapter(id);
        Optional<Chapter> latestChapter = this.novelService.getLatestChapter(id);

        Chapter first = firstChapter.orElse(null);
        Chapter latest = latestChapter.orElse(null);

        String breadcrumbFrom = from
                .map(String::toLowerCase)
                .orElseGet(() -> {
                    String referer = request.getHeader("referer");
                    if (referer == null) {
                        return "home";
                    }
                    String refererLower = referer.toLowerCase();
                    if (refererLower.contains("/category")) {
                        return "category";
                    }
                    if (refererLower.contains("/library")
                            || refererLower.contains("/bookshelf")
                            || refererLower.contains("/bookmark")) {
                        return "library";
                    }
                    return "home";
                });

        model.addAttribute("chapters", chapters);
        model.addAttribute("novel", novel);
        model.addAttribute("firstChapter", first);
        model.addAttribute("latestChapter", latest);
        model.addAttribute("breadcrumbFrom", breadcrumbFrom);
        boolean inLibrary = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && this.libraryService.isNovelInLibrary(authentication.getName(), id);
        model.addAttribute("inLibrary", inLibrary);

        return "client/novel/show";
    }
}
