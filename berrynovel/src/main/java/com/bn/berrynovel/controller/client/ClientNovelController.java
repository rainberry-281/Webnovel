package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.service.PaginationService;

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

    public ClientNovelController(NovelService novelService, PaginationService paginationService) {
        this.novelService = novelService;
        this.paginationService = paginationService;
    }

    @GetMapping("/category")
    public String getCategoryPage(Model model) {
        List<Novel> novels = this.novelService.getNovels();
        model.addAttribute("novels", novels);
        model.addAttribute("genres", this.novelService.getAllGenres());
        return "client/category/show";
    }

    @GetMapping("/novel/{id}")
    public String getNovelDetailPage(@PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) Optional<String> from,
            Model model, HttpServletRequest request) {
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

        return "client/novel/show";
    }
}
