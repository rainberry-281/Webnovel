package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bn.berrynovel.service.CommentService;
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
    private static final Logger logger = LoggerFactory.getLogger(ClientNovelController.class);
    private static final String LOG_DIVIDER = "============================================================";
    private final NovelService novelService;
    private final PaginationService paginationService;
    private final LibraryService libraryService;
    private final CommentService commentService;

    public ClientNovelController(NovelService novelService, PaginationService paginationService,
            LibraryService libraryService, CommentService commentService) {
        this.novelService = novelService;
        this.paginationService = paginationService;
        this.libraryService = libraryService;
        this.commentService = commentService;
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
        model.addAttribute("genres", this.novelService.getActiveGenres());
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

        logger.info(
                "\n{}\n>>>>>>>>>>> [SEARCH NOVEL - REQUEST]\nkeyword={}\npage={}\ngenres={}\ntypes={}\nprogresses={}\n{}\n",
                LOG_DIVIDER,
                normalizedKeyword,
                pageOptional.orElse("1"),
                selectedGenres,
                selectedTypes,
                selectedProgresses,
                LOG_DIVIDER);

        if (normalizedKeyword.isEmpty() && selectedGenres.isEmpty() && selectedTypes.isEmpty()
                && selectedProgresses.isEmpty()) {
            logger.info("\n{}\n>>>>>>>>>>> [SEARCH NOVEL - EMPTY FILTER]\nredirect=/category\n{}\n",
                    LOG_DIVIDER,
                    LOG_DIVIDER);
            return "redirect:/category";
        }

        PaginationQuery<Novel> nvs = this.paginationService.ClientFilterNovelPagination(pageOptional, 20,
                normalizedKeyword, selectedGenres, selectedTypes, selectedProgresses);

        logger.info("\n{}\n>>>>>>>>>>> [SEARCH NOVEL - RESULT]\ncurrentPage={}\ntotalPage={}\nresultCount={}\n{}\n",
                LOG_DIVIDER,
                nvs.getPage(),
                nvs.getNvs().getTotalPages(),
                nvs.getNvs().getContent().size(),
                LOG_DIVIDER);

        model.addAttribute("novels", nvs.getNvs().getContent());
        model.addAttribute("currentPage", nvs.getPage());
        model.addAttribute("totalPage", nvs.getNvs().getTotalPages());
        model.addAttribute("genres", this.novelService.getActiveGenres());
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

        logger.info("\n{}\n>>>>>>>>>>> [NOVEL DETAIL - ACCESS]\nid={}\ntitle={}\npath={}\nuser={}\n{}\n",
                LOG_DIVIDER,
                id,
                novel.getTitle(),
                request.getRequestURI(),
                authentication == null ? "anonymous" : authentication.getName(),
                LOG_DIVIDER);

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
        model.addAttribute("comments", this.commentService.getCommentsByNovelId(id));
        boolean inLibrary = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && this.libraryService.isNovelInLibrary(authentication.getName(), id);
        model.addAttribute("inLibrary", inLibrary);

        return "client/novel/show";
    }

    @PostMapping("/novel/{id}/comment")
    public String createComment(@PathVariable("id") Long novelId,
            @RequestParam("content") String content,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        logger.info("\n{}\n>>>>>>>>>>> [CREATE COMMENT - REQUEST]\nnovelId={}\nuser={}\ncontentLength={}\n{}\n",
                LOG_DIVIDER,
                novelId,
                authentication.getName(),
                content == null ? 0 : content.length(),
                LOG_DIVIDER);

        this.commentService.createComment(novelId, authentication.getName(), content);
        logger.info("\n{}\n>>>>>>>>>>> [CREATE COMMENT - SUCCESS]\nnovelId={}\nuser={}\n{}\n",
                LOG_DIVIDER,
                novelId,
                authentication.getName(),
                LOG_DIVIDER);
        return "redirect:/novel/" + novelId + "#comments";
    }

    @PostMapping("/novel/{novelId}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable("novelId") Long novelId,
            @PathVariable("commentId") Integer commentId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_AMIN".equalsIgnoreCase(role));

        logger.info("\n{}\n>>>>>>>>>>> [DELETE COMMENT - REQUEST]\nnovelId={}\ncommentId={}\nuser={}\n{}\n",
                LOG_DIVIDER,
                novelId,
                commentId,
                authentication.getName(),
                LOG_DIVIDER);

        this.commentService.deleteComment(novelId, commentId, authentication.getName(), isAdmin);
        logger.info("\n{}\n>>>>>>>>>>> [DELETE COMMENT - SUCCESS]\nnovelId={}\ncommentId={}\nuser={}\n{}\n",
                LOG_DIVIDER,
                novelId,
                commentId,
                authentication.getName(),
                LOG_DIVIDER);
        return "redirect:/novel/" + novelId + "#comments";
    }
}
