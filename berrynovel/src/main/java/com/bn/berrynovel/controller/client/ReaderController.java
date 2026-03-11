package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import java.util.List;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.service.NovelService;

@Controller
@RequestMapping("/reader")
public class ReaderController {
    private final NovelService novelService;

    public ReaderController(NovelService novelService) {
        this.novelService = novelService;
    }

    @GetMapping("/{novelID}/{chapterID}")
    public String getReaderPage(@PathVariable Long novelID, @PathVariable Long chapterID, Model model) {
        Chapter chapter = this.novelService.getChapterById(chapterID);

        Novel novel = this.novelService.getNovelById(novelID)

                .orElseThrow(() -> new RuntimeException("Novel not found"));
        List<Chapter> chapters = this.novelService.getChaptersByNovelId(novelID);

        Chapter prev = null;
        Chapter next = null;

        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getId().equals(chapterID)) {

                if (i > 0) {
                    prev = chapters.get(i - 1);
                }

                if (i < chapters.size() - 1) {
                    next = chapters.get(i + 1);
                }

                break;
            }
        }

        model.addAttribute("chapter", chapter);
        model.addAttribute("novel", novel);
        model.addAttribute("chapters", chapters);
        model.addAttribute("prev", prev);
        model.addAttribute("next", next);
        return "/client/reader/show";
    }

}
