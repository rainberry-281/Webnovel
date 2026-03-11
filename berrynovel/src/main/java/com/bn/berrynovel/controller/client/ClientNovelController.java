package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.bn.berrynovel.service.NovelService;

import org.springframework.ui.Model;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.domain.Chapter;
import java.util.List;

@Controller
public class ClientNovelController {
    private final NovelService novelService;

    public ClientNovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    @GetMapping("/novel/{id}")
    public String getNovelDetailPage(@PathVariable("id") Long id, Model model) {
        Novel novel = this.novelService.getNovelById(id).orElseThrow(() -> new RuntimeException("Novel not found"));

        List<Chapter> chapters = this.novelService.getChaptersByNovelId(id);

        model.addAttribute("chapters", chapters);
        model.addAttribute("novel", novel);
        return "client/novel/show";
    }
}
