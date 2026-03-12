package com.bn.berrynovel.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.service.NovelService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Path;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/chapter")
public class ChapterController {
    private final NovelService novelService;
    private final ChapterRepository chapterRepository;

    public ChapterController(NovelService novelService, ChapterRepository chapterRepository) {
        this.novelService = novelService;
        this.chapterRepository = chapterRepository;
    }

    // @GetMapping("/create/{novelId}")
    // public String listByNovel(@PathVariable("novelId") int novelId, Model model)
    // {
    // Novel novel = this.novelService.getNovelById(novelId)
    // .orElseThrow(() -> new RuntimeException("Novel not found"));

    // List<Chapter> chapters = this.novelService.getChaptersByNovelId(novelId);

    // model.addAttribute("novel", novel);
    // model.addAttribute("chapters", chapters);
    // model.addAttribute("newChapter", new Chapter());
    // return "admin/chapter/list";
    // }

    @GetMapping("/create/{novelId}")
    public String createPage(@PathVariable("novelId") Long novelId, Model model) {
        Novel novel = this.novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        List<Chapter> chapters = this.novelService.getChaptersByNovelId(novelId);

        model.addAttribute("novel", novel);
        model.addAttribute("chapters", chapters);
        model.addAttribute("newChapter", new Chapter());
        return "admin/chapter/create";
    }

    @PostMapping("/create/{novelId}")
    public String create(@PathVariable("novelId") Long novelId, Chapter chapter) {
        Novel novel = this.novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        chapter.setNovel(novel);
        chapter.setCreatedAt(java.time.LocalDateTime.now());
        this.chapterRepository.save(chapter);

        return "redirect:/admin/chapter/create/" + novelId;
    }

    @GetMapping("/update/{id}")
    public String updatePage(@PathVariable Long id, Model model) {

        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        Novel novel = chapter.getNovel();

        List<Chapter> chapters = this.novelService.getChaptersByNovelId(novel.getId());

        model.addAttribute("chapter", chapter);
        model.addAttribute("novel", novel);
        model.addAttribute("chapters", chapters);

        return "admin/chapter/update";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id, Chapter chapter) {

        Chapter oldChapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        oldChapter.setTitle(chapter.getTitle());
        oldChapter.setContent(chapter.getContent());

        chapterRepository.save(oldChapter);

        return "redirect:/admin/chapter/create/" + oldChapter.getNovel().getId();
    }

    // @PostMapping("/delete/{id}")
    // public String delete(@PathVariable("id") Long id) {
    // Chapter chapter = this.chapterRepository.findById(id)
    // .orElseThrow(() -> new RuntimeException("Chapter not found"));

    // int novelId = chapter.getNovel().getId();
    // this.chapterRepository.deleteById(id);

    // return "redirect:/admin/chapter/create/" + novelId;
    // }

    @PostMapping("/upload-image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("upload") MultipartFile file) throws IOException {

        String uploadDir = System.getProperty("user.dir")
                + "/src/main/resources/static/images/chapter/";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        File dir = new File(uploadDir);
        if (!dir.exists())
            dir.mkdirs();

        File dest = new File(uploadDir + fileName);
        file.transferTo(dest);

        Map<String, Object> result = new HashMap<>();
        result.put("url", "/images/chapter/" + fileName);

        return result;
    }
}