package com.bn.berrynovel.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    public Page<Novel> findByTitleContaining(String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return this.novelRepository.findAll(pageable);
        }
        return this.novelRepository.findByTitleContainingIgnoreCase(normalizedKeyword, pageable);
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

    public Page<Novel> findActiveNovels(Pageable pageable) {
        return this.novelRepository.findByStatus(true, pageable);
    }

    public List<Novel> searchVisibleNovels(String keyword, List<String> genreCodes, List<String> typeValues,
            List<String> progressValues) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<String> normalizedGenreCodes = genreCodes == null
                ? List.of()
                : genreCodes.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(code -> !code.isEmpty())
                        .distinct()
                        .toList();
        List<String> normalizedTypeValues = typeValues == null
                ? List.of()
                : typeValues.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .distinct()
                        .toList();
        List<String> normalizedProgressValues = progressValues == null
                ? List.of()
                : progressValues.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .distinct()
                        .toList();

        if (normalizedKeyword.isEmpty() && normalizedGenreCodes.isEmpty() && normalizedTypeValues.isEmpty()
                && normalizedProgressValues.isEmpty()) {
            return List.of();
        }

        StringBuilder filter = new StringBuilder("status : true and genres.status : true");
        if (!normalizedKeyword.isEmpty()) {
            String escapedKeyword = normalizedKeyword
                    .replace("\\", "\\\\")
                    .replace("'", "\\'");
            filter.append(" and title ~~ '%").append(escapedKeyword).append("%'");
        }

        if (!normalizedGenreCodes.isEmpty()) {
            String genreInClause = normalizedGenreCodes.stream()
                    .map(code -> code.replace("\\", "\\\\").replace("'", "\\'"))
                    .map(code -> "'" + code + "'")
                    .collect(java.util.stream.Collectors.joining(", "));
            filter.append(" and genres.code in [").append(genreInClause).append("]");
        }

        if (!normalizedTypeValues.isEmpty()) {
            String typeInClause = normalizedTypeValues.stream()
                    .map(value -> value.replace("\\", "\\\\").replace("'", "\\'"))
                    .map(value -> "'" + value + "'")
                    .collect(java.util.stream.Collectors.joining(", "));
            filter.append(" and type in [").append(typeInClause).append("]");
        }

        if (!normalizedProgressValues.isEmpty()) {
            String progressInClause = normalizedProgressValues.stream()
                    .map(value -> value.replace("\\", "\\\\").replace("'", "\\'"))
                    .map(value -> "'" + value + "'")
                    .collect(java.util.stream.Collectors.joining(", "));
            filter.append(" and progress in [").append(progressInClause).append("]");
        }

        Specification<Novel> specification = this.filterSpecificationConverter.convert(filter.toString());
        Pageable pageable = PageRequest.of(0, 500);

        return this.novelRepository.findAll(specification, pageable).getContent().stream()
                .filter(novel -> novel.getGenres() != null && !novel.getGenres().isEmpty())
                .filter(novel -> novel.getGenres().stream().allMatch(Genre::getStatus))
                .filter(novel -> {
                    if (normalizedGenreCodes.isEmpty()) {
                        return true;
                    }
                    java.util.Set<String> novelGenreCodes = novel.getGenres().stream()
                            .map(Genre::getCode)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .collect(java.util.stream.Collectors.toSet());
                    return novelGenreCodes.containsAll(normalizedGenreCodes);
                })
                .collect(java.util.stream.Collectors.toMap(
                        Novel::getId,
                        novel -> novel,
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(novel -> {
                    Chapter latest = this.chapterRepository.findLastChapter(novel.getId());
                    return latest != null ? latest.getCreatedAt() : LocalDateTime.MIN;
                }, Comparator.reverseOrder()))
                .toList();
    }

    public Page<Novel> searchVisibleNovels(String keyword, List<String> genreCodes, List<String> typeValues,
            List<String> progressValues, Pageable pageable) {
        List<Novel> searchedNovels = this.searchVisibleNovels(keyword, genreCodes, typeValues, progressValues);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), searchedNovels.size());
        List<Novel> content = start >= searchedNovels.size()
                ? List.of()
                : searchedNovels.subList(start, end);

        return new PageImpl<>(content, pageable, searchedNovels.size());
    }

    public List<Novel> searchVisibleNovelsByTitle(String keyword) {
        return this.searchVisibleNovels(keyword, List.of(), List.of(), List.of());
    }

    public Page<Novel> searchVisibleNovelsByTitle(String keyword, Pageable pageable) {
        return this.searchVisibleNovels(keyword, List.of(), List.of(), List.of(), pageable);
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
