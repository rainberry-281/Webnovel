package com.bn.berrynovel.controller.client;

import java.util.List;

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
@RequestMapping("/library")
public class ClientLibraryController {
    private final LibraryService libraryService;

    public ClientLibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping({ "", "/", "/bookshelf" })
    public String GetLibraryPage(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("libraryItems", this.libraryService.getBookshelfItems(username));
        return "client/library/bookshelf";
    }

    @PostMapping("/toggle/{novelId}")
    public String toggleNovelInLibrary(@PathVariable("novelId") Long novelId, Authentication authentication) {
        this.libraryService.toggleNovelInLibrary(authentication.getName(), novelId);
        return "redirect:/novel/" + novelId + "?from=library";
    }

    @PostMapping("/bookshelf/delete")
    public String deleteFromLibrary(@RequestParam(value = "novelIds", required = false) List<Long> novelIds,
            Authentication authentication) {
        this.libraryService.deleteNovelsFromLibrary(authentication.getName(), novelIds);
        return "redirect:/library/bookshelf";
    }
}
