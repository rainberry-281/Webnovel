package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.service.LibraryService;
import com.bn.berrynovel.service.NovelService;

@Controller
@RequestMapping("/reader")
public class ReaderController {
    private static final Logger logger = LoggerFactory.getLogger(ReaderController.class);
    private static final String LOG_DIVIDER = "============================================================";

    private final NovelService novelService;
    private final LibraryService libraryService;

    public ReaderController(NovelService novelService, LibraryService libraryService) {
        this.novelService = novelService;
        this.libraryService = libraryService;
    }

    @GetMapping("/{novelID}/{chapterID}")
    public String getReaderPage(@PathVariable Long novelID, @PathVariable Long chapterID, Model model,
            Authentication authentication, HttpServletRequest request) {
        Chapter chapter = this.novelService.getChapterById(chapterID);

        Novel novel = this.novelService.getNovelById(novelID)

                .orElseThrow(() -> new RuntimeException("Novel not found"));

        logger.info(
                "\n{}\n>>>>>>>>>>> [READER - ACCESS]\nnovelId={}\nnovelTitle={}\nchapterId={}\nchapterTitle={}\npath={}\nuser={}\n{}\n",
                LOG_DIVIDER,
                novelID,
                novel.getTitle(),
                chapterID,
                chapter == null ? null : chapter.getTitle(),
                request.getRequestURI(),
                authentication == null ? "anonymous" : authentication.getName(),
                LOG_DIVIDER);

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
        boolean isBookmarked = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && this.libraryService.isChapterBookmarked(authentication.getName(), chapterID);
        model.addAttribute("isBookmarked", isBookmarked);
        return "/client/reader/show";
    }

}
