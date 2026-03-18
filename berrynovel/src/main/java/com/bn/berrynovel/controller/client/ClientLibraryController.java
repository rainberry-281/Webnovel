package com.bn.berrynovel.controller.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bn.berrynovel.service.LibraryService;

import org.springframework.ui.Model;

@Controller
@RequestMapping({ "/bookshelf", "/library" })
public class ClientLibraryController {
    private static final Logger logger = LoggerFactory.getLogger(ClientLibraryController.class);
    private static final String LOG_DIVIDER = "============================================================";

    private final LibraryService libraryService;

    public ClientLibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping({ "", "/", "/bookshelf" })
    public String getBookshelfPage(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("bookshelfItems", this.libraryService.getBookshelfItems(username));
        return "client/library/bookshelf";
    }

    @GetMapping("/bookmark")
    public String getBookmarkPage(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("bookmarkItems", this.libraryService.getBookmarkItems(username));
        return "client/library/bookmark";
    }

    @PostMapping("/toggle/{novelId}")
    public String toggleNovelInBookshelf(@PathVariable("novelId") Long novelId, Authentication authentication) {

        logger.info("\n{}\n>>>>>>>>>>> [BOOKSHELF TOGGLE - REQUEST]\nuser={}\nnovelId={}\n{}\n",
                LOG_DIVIDER,
                authentication.getName(),
                novelId,
                LOG_DIVIDER);

        this.libraryService.toggleNovelInBookshelf(authentication.getName(), novelId);

        logger.info("\n{}\n>>>>>>>>>>> [BOOKSHELF TOGGLE - SUCCESS]\nuser={}\nnovelId={}\n{}\n",
                LOG_DIVIDER,
                authentication.getName(),
                novelId,
                LOG_DIVIDER);
        return "redirect:/novel/" + novelId + "?from=bookshelf";
    }

    @PostMapping("/bookmark/toggle/{novelId}/{chapterId}")
    public String toggleChapterBookmark(@PathVariable("novelId") Long novelId,
            @PathVariable("chapterId") Long chapterId,
            Authentication authentication) {
        logger.info("\n{}\n>>>>>>>>>>> [BOOKMARK TOGGLE - REQUEST]\nuser={}\nnovelId={}\nchapterId={}\n{}\n",
                LOG_DIVIDER,
                authentication.getName(),
                novelId,
                chapterId,
                LOG_DIVIDER);
        this.libraryService.toggleChapterBookmark(authentication.getName(), novelId, chapterId);
        logger.info("\n{}\n>>>>>>>>>>> [BOOKMARK TOGGLE - SUCCESS]\nuser={}\nnovelId={}\nchapterId={}\n{}\n",
                LOG_DIVIDER,
                authentication.getName(),
                novelId,
                chapterId,
                LOG_DIVIDER);
        return "redirect:/reader/" + novelId + "/" + chapterId;
    }

    @PostMapping({ "/delete", "/bookshelf/delete" })
    public String deleteFromBookshelf(@RequestParam(value = "novelIds", required = false) List<Long> novelIds,
            Authentication authentication) {
        logger.info("\n{}\n>>>>>>>>>>> [BOOKSHELF DELETE - REQUEST]\nuser={}\nnovelIds={}\n{}\n",
                LOG_DIVIDER,
                authentication.getName(),
                novelIds,
                LOG_DIVIDER);
        this.libraryService.deleteNovelsFromBookshelf(authentication.getName(), novelIds);
        logger.info("\n{}\n>>>>>>>>>>> [BOOKSHELF DELETE - SUCCESS]\nuser={}\nnovelIds={}\n{}\n",
                LOG_DIVIDER,
                authentication.getName(),
                novelIds,
                LOG_DIVIDER);
        return "redirect:/bookshelf";
    }
}
