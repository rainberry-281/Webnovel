package com.bn.berrynovel.service;

import java.util.Optional;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.repository.NovelRepository;
import com.bn.berrynovel.domain.Genre;

@Service
public class NovelService {
    private final NovelRepository novelRepository;
    private final GenreRepository genreRepository;
    private final ImageService imageService;

    public NovelService(NovelRepository novelRepository, GenreRepository genreRepository, ImageService imageService) {
        this.novelRepository = novelRepository;
        this.genreRepository = genreRepository;
        this.imageService = imageService;
    }

    public void updateNovel(Novel novel, MultipartFile file) {
        // if (novel.getGenres() != null && !novel.getGenres().isEmpty()) {
        // Genre genreInDataBase =
        // this.genreRepository.findByCode(novel.getGenres().get(0).getCode());
        // }
        // novel.setGenres(novel.getGenres().stream().map(g ->
        // this.genreRepository.findByCode(g.getCode())).toList());
        String imageName = "";
        if (file != null && !file.isEmpty()) {
            imageName = this.imageService.handleImage(file, "novel");
            novel.setImage(imageName);
        }
        novel.setImage(imageName);

        Novel savedNovel = this.novelRepository.save(novel);
    }

    public List<Novel> getAllNovels() {
        return this.novelRepository.findAll();
    }

    // public List<Novel> getActiveNovels() {
    // return this.novelRepository.findByStatus(true);
    // }

    public Optional<Novel> getNovelById(int id) {
        return this.novelRepository.findById(id);
    }

    public List<Genre> getAllGenres() {
        return this.genreRepository.findAll();
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

    public void toggleNovelStatus(int id) {
        Novel novelInDataBase = this.novelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Novel not found"));
        novelInDataBase.setStatus(!novelInDataBase.getStatus());
        this.novelRepository.save(novelInDataBase);
    }
}
