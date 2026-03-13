package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.bn.berrynovel.service.NovelService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.Chapter;
import java.util.List;
import java.util.Optional;

@Controller
public class ClientNovelController {
    private final NovelService novelService;

    public ClientNovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    @GetMapping("/novel/{id}")
    public String getNovelDetailPage(@PathVariable("id") Long id, Model model, HttpServletRequest request) {
        Novel novel = this.novelService.getNovelById(id).orElseThrow(() -> new RuntimeException("Novel not found"));

        List<Chapter> chapters = this.novelService.getChaptersByNovelId(id);

        Optional<Chapter> firstChapter = this.novelService.getFirstChapter(id);
        Optional<Chapter> latestChapter = this.novelService.getLatestChapter(id);

        Chapter first = firstChapter.orElse(null);
        Chapter latest = latestChapter.orElse(null);

        model.addAttribute("chapters", chapters);
        model.addAttribute("novel", novel);
        model.addAttribute("firstChapter", first);
        model.addAttribute("latestChapter", latest);

        return "client/novel/show";
    }
}
