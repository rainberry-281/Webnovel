package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.ui.Model;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.service.CommentService;
import com.bn.berrynovel.service.LibraryService;
import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.service.ReadCountService;
import com.bn.berrynovel.service.UrlSlugService;

@Controller
public class ReaderController {
    private static final Logger logger = LoggerFactory.getLogger(ReaderController.class);
    private static final String LOG_DIVIDER = "============================================================";

    private final NovelService novelService;
    private final LibraryService libraryService;
    private final ReadCountService readCountService;
    private final CommentService commentService;
    private final UrlSlugService urlSlugService;

    public ReaderController(NovelService novelService, LibraryService libraryService, ReadCountService readCountService,
            CommentService commentService, UrlSlugService urlSlugService) {
        this.novelService = novelService;
        this.libraryService = libraryService;
        this.readCountService = readCountService;
        this.commentService = commentService;
        this.urlSlugService = urlSlugService;
    }

    @GetMapping("/reader/{novelID}/{chapterID}")
    public RedirectView redirectLegacyReaderPage(@PathVariable Long novelID, @PathVariable Long chapterID,
            HttpServletRequest request) {
        Novel novel = findNovelOr404(novelID);
        Chapter chapter = findChapterOr404(chapterID);
        validateChapterBelongsToNovel(chapter, novelID);

        return permanentRedirect(appendQueryString(this.urlSlugService.buildReaderUrl(novel, chapter), request));
    }

    @GetMapping("/{novelSlug:\\d+-[a-z0-9-]+}/{chapterSlug:c\\d+-[a-z0-9-]+}")
    public Object readChapterBySlug(@PathVariable String novelSlug,
            @PathVariable String chapterSlug,
            Model model,
            Authentication authentication,
            HttpServletRequest request) {
        Long novelID = this.urlSlugService.extractNovelIdFromSlug(novelSlug);
        Long chapterID = this.urlSlugService.extractChapterIdFromSlug(chapterSlug);
        if (novelID == null || chapterID == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Novel novel = findNovelOr404(novelID);
        Chapter chapter = findChapterOr404(chapterID);
        validateChapterBelongsToNovel(chapter, novelID);

        String canonicalReaderUrl = this.urlSlugService.buildReaderUrl(novel, chapter);
        if (!canonicalReaderUrl.equals(request.getRequestURI())) {
            return permanentRedirect(appendQueryString(canonicalReaderUrl, request));
        }

        return renderReaderPage(novel, chapter, model, authentication, request);
    }

    private String renderReaderPage(Novel novel, Chapter chapter, Model model,
            Authentication authentication, HttpServletRequest request) {
        Long novelID = novel.getId();
        Long chapterID = chapter.getId();

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

        this.readCountService.recordChapterRead(novelID, chapterID, authentication, request);

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

        Integer savedLinePosition = null;
        String savedParagraphKey = null;
        if (isBookmarked) {
            savedLinePosition = this.libraryService.getBookmarkLinePosition(authentication.getName(), chapterID);
            savedParagraphKey = this.libraryService.getBookmarkParagraphKey(authentication.getName(), chapterID);
        }
        model.addAttribute("savedLinePosition", savedLinePosition);
        model.addAttribute("savedParagraphKey", savedParagraphKey);
        model.addAttribute("chapterComments", this.commentService.getCommentsByChapterId(chapterID));
        model.addAttribute("canonicalReaderUrl", this.urlSlugService.buildReaderUrl(novel, chapter));
        return "/client/reader/show";
    }

    @PostMapping("/{novelSlug:\\d+-[a-z0-9-]+}/{chapterSlug:c\\d+-[a-z0-9-]+}/comments")
    public String createChapterCommentBySlug(@PathVariable String novelSlug,
            @PathVariable String chapterSlug,
            @RequestParam("content") String content,
            Authentication authentication) {
        Long novelID = this.urlSlugService.extractNovelIdFromSlug(novelSlug);
        Long chapterID = this.urlSlugService.extractChapterIdFromSlug(chapterSlug);
        if (novelID == null || chapterID == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return createChapterComment(novelID, chapterID, content, authentication);
    }

    @PostMapping("/reader/{novelID}/{chapterID}/comments")
    public String createLegacyChapterComment(@PathVariable Long novelID,
            @PathVariable Long chapterID,
            @RequestParam("content") String content,
            Authentication authentication) {
        return createChapterComment(novelID, chapterID, content, authentication);
    }

    private String createChapterComment(Long novelID,
            Long chapterID,
            String content,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        Novel novel = findNovelOr404(novelID);
        Chapter chapter = findChapterOr404(chapterID);
        validateChapterBelongsToNovel(chapter, novelID);

        this.commentService.createChapterComment(novelID, chapterID, authentication.getName(), content);
        return "redirect:" + this.urlSlugService.buildReaderUrl(novel, chapter) + "#comments";
    }

    private Novel findNovelOr404(Long novelID) {
        return this.novelService.getNovelById(novelID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Chapter findChapterOr404(Long chapterID) {
        return this.novelService.findChapterById(chapterID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateChapterBelongsToNovel(Chapter chapter, Long novelID) {
        if (chapter.getNovel() == null || chapter.getNovel().getId() == null
                || !chapter.getNovel().getId().equals(novelID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private RedirectView permanentRedirect(String url) {
        RedirectView redirectView = new RedirectView(url);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }

    private String appendQueryString(String url, HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return url;
        }
        return url + "?" + queryString;
    }

}
