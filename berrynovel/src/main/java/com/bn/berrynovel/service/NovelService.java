package com.bn.berrynovel.service;

import java.util.Comparator;
import java.util.Optional;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.repository.NovelRepository;
import com.bn.berrynovel.domain.Genre;
import com.bn.berrynovel.domain.Chapter;
import com.bn.berrynovel.repository.ChapterRepository;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;

@Service
public class NovelService {
    private final NovelRepository novelRepository;
    private final GenreRepository genreRepository;
    private final ChapterRepository chapterRepository;
    private final ImageService imageService;
    private final FilterSpecificationConverter filterSpecificationConverter;

    public NovelService(NovelRepository novelRepository, GenreRepository genreRepository, ImageService imageService,
            ChapterRepository chapterRepository, FilterSpecificationConverter filterSpecificationConverter) {
        this.novelRepository = novelRepository;
        this.genreRepository = genreRepository;
        this.imageService = imageService;
        this.chapterRepository = chapterRepository;
        this.filterSpecificationConverter = filterSpecificationConverter;
    }

    public void createNovel(Novel novel, MultipartFile file) {
        if (novel.getGenres() != null) {
            List<Integer> genreIds = novel.getGenres().stream()
                    .map(Genre::getId)
                    .filter(Objects::nonNull)
                    .toList();
            List<Genre> managedGenres = genreIds.isEmpty()
                    ? List.of()
                    : this.genreRepository.findAllById(genreIds);
            novel.setGenres(managedGenres);
        } else {
            novel.setGenres(List.of());
        }

        String imageName = "";
        if (file != null && !file.isEmpty()) {
            imageName = this.imageService.handleImage(file, "novel");
            novel.setImage(imageName);
        }

        Novel savedNovel = this.novelRepository.save(novel);
    }

    @Transactional
    public void updateNovel(Novel novel, MultipartFile file) {
        Novel novelInDataBase = this.novelRepository.findById(novel.getId())
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        if (file != null && !file.isEmpty()) {
            this.imageService.deleteImage(novelInDataBase.getImage(), "novel");
            String imageName = this.imageService.handleImage(file, "novel");
            novelInDataBase.setImage(imageName);
        }
        novelInDataBase.setTitle(novel.getTitle());
        novelInDataBase.setAuthor(novel.getAuthor());
        novelInDataBase.setDescription(novel.getDescription());
        if (novel.getGenres() != null) {
            List<Integer> genreIds = novel.getGenres().stream()
                    .map(Genre::getId)
                    .filter(Objects::nonNull)
                    .toList();
            List<Genre> managedGenres = genreIds.isEmpty()
                    ? List.of()
                    : this.genreRepository.findAllById(genreIds);
            novelInDataBase.setGenres(managedGenres);
        }
        novelInDataBase.setType(novel.getType());
        novelInDataBase.setProgress(novel.getProgress());
        this.novelRepository.save(novelInDataBase);
    }

    public List<Novel> getAllNovels() {
        return this.novelRepository.findAll();
    }

    public Page<Novel> findAll(Pageable pageable) {
        return this.novelRepository.findAll(pageable);
    }

    // public List<Novel> getActiveNovels() {
    // return this.novelRepository.findByStatus(true);
    // }

    public Optional<Novel> getNovelById(long id) {
        return this.novelRepository.findById(id);
    }

    public List<Genre> getAllGenres() {
        return this.genreRepository.findAllOrderByNameAsc();
    }

    public Page<Genre> findAllGenres(Pageable pageable) {
        return this.genreRepository.findAll(pageable);
    }

    public void saveGenre(Genre genre) {
        this.genreRepository.save(genre);
    }

    public void actionGenre(int id) {
        Genre genreInDataBase = this.genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found"));
        genreInDataBase.setStatus(!genreInDataBase.getStatus());
        this.genreRepository.save(genreInDataBase);
    }

    public void updateGenre(Genre genre) {
        Genre genreInDataBase = this.genreRepository.findById(genre.getId())
                .orElseThrow(() -> new RuntimeException("Genre not found"));
        if (genreInDataBase != null) {
            genreInDataBase.setName(genre.getName());
            genreInDataBase.setCode(genre.getCode());
            this.genreRepository.save(genreInDataBase);
        }
    }

    public void toggleNovelStatus(Long id) {
        Novel novelInDataBase = this.novelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        novelInDataBase.setStatus(!novelInDataBase.getStatus());
        this.novelRepository.save(novelInDataBase);
    }

    public List<Novel> getNovels() {
        return this.novelRepository.findByStatus(true);
    }

    public List<Novel> getHomepageCompletedNovels() {
        String filter = "status : true and progress : 'COMPLETED' and genres.status : true";
        Specification<Novel> specification = this.filterSpecificationConverter.convert(filter);
        Pageable pageable = PageRequest.of(0, 100);
        return this.novelRepository.findAll(specification, pageable).getContent().stream()
                .filter(novel -> novel.getGenres() != null && !novel.getGenres().isEmpty())
                .filter(novel -> novel.getGenres().stream().allMatch(Genre::getStatus))
                .sorted(Comparator.comparing(novel -> {
                    Chapter latest = this.chapterRepository.findLastChapter(novel.getId());
                    return latest != null ? latest.getCreatedAt() : LocalDateTime.MIN;
                }, Comparator.reverseOrder()))
                .limit(6)
                .toList();
    }

    public List<Novel> getHomepageOriginalNovels() {
        String filter = "status : true and type : 'ORIGINAL' and genres.status : true";
        Specification<Novel> specification = this.filterSpecificationConverter.convert(filter);
        Pageable pageable = PageRequest.of(0, 100);
        return this.novelRepository.findAll(specification, pageable).getContent().stream()
                .filter(novel -> novel.getGenres() != null && !novel.getGenres().isEmpty())
                .filter(novel -> novel.getGenres().stream().allMatch(Genre::getStatus))
                .sorted(Comparator.comparing(novel -> {
                    Chapter latest = this.chapterRepository.findLastChapter(novel.getId());
                    return latest != null ? latest.getCreatedAt() : LocalDateTime.MIN;
                }, Comparator.reverseOrder()))
                .limit(6)
                .toList();
    }

    public List<Novel> getHomepageTranslatedNovels() {
        String filter = "status : true and type : 'TRANSLATION' and genres.status : true";
        Specification<Novel> specification = this.filterSpecificationConverter.convert(filter);
        Pageable pageable = PageRequest.of(0, 100);
        return this.novelRepository.findAll(specification, pageable).getContent().stream()
                .filter(novel -> novel.getGenres() != null && !novel.getGenres().isEmpty())
                .filter(novel -> novel.getGenres().stream().allMatch(Genre::getStatus))
                .sorted(Comparator.comparing(novel -> {
                    Chapter latest = this.chapterRepository.findLastChapter(novel.getId());
                    return latest != null ? latest.getCreatedAt() : LocalDateTime.MIN;
                }, Comparator.reverseOrder()))
                .limit(6)
                .toList();
    }

    public List<Chapter> getChaptersByNovelId(Long novelId) {
        return this.chapterRepository.findByNovelId(novelId);
    }

    public Chapter getChapterById(Long id) {
        return this.chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
    }

    public Optional<Chapter> getFirstChapter(Long novelId) {
        return Optional.ofNullable(this.chapterRepository.findFirstChapter(novelId));
    }

    public Optional<Chapter> getLatestChapter(Long novelId) {
        return Optional.ofNullable(this.chapterRepository.findLastChapter(novelId));
    }
}
