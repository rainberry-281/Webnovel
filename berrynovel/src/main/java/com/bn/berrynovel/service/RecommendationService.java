package com.bn.berrynovel.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.repository.NovelRepository;

@Service
public class RecommendationService {

    private static final int PREFERRED_GENRE_LIMIT = 5;
    private static final int SIGNAL_LOOKBACK_DAYS = 60;

    private final NovelRepository novelRepository;

    public RecommendationService(NovelRepository novelRepository) {
        this.novelRepository = novelRepository;
    }

    public List<Novel> getSimilarNovels(Long novelId, int limit) {
        if (novelId == null || limit <= 0) {
            return List.of();
        }
        return this.novelRepository.findSimilarNovels(novelId, PageRequest.of(0, limit));
    }

    public List<Novel> getPersonalizedRecommendations(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(SIGNAL_LOOKBACK_DAYS);
        List<Integer> preferredGenreIds = this.novelRepository.findPreferredGenreIds(
                userId,
                since,
                PageRequest.of(0, PREFERRED_GENRE_LIMIT));

        List<Novel> recommendations = preferredGenreIds.isEmpty()
                ? List.of()
                : this.novelRepository.findRecommendedByPreferredGenres(userId, preferredGenreIds,
                        PageRequest.of(0, limit));

        return fillWithPopular(recommendations, limit);
    }

    public List<Novel> getPopularNovels(int days, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        int normalizedDays = Math.max(days, 1);
        LocalDateTime since = LocalDateTime.now().minusDays(normalizedDays);
        return this.novelRepository.findPopularRecommendations(since, PageRequest.of(0, limit));
    }

    public List<Novel> getTopRatedNovels(int minimumRatings, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return this.novelRepository.findTopRatedRecommendations(Math.max(minimumRatings, 1), PageRequest.of(0, limit));
    }

    private List<Novel> fillWithPopular(List<Novel> primaryRecommendations, int limit) {
        Map<Long, Novel> merged = new LinkedHashMap<>();

        for (Novel novel : primaryRecommendations) {
            if (novel != null && novel.getId() != null) {
                merged.putIfAbsent(novel.getId(), novel);
            }
        }

        if (merged.size() < limit) {
            for (Novel novel : getPopularNovels(30, limit * 2)) {
                if (novel != null && novel.getId() != null) {
                    merged.putIfAbsent(novel.getId(), novel);
                }
                if (merged.size() >= limit) {
                    break;
                }
            }
        }

        return new ArrayList<>(merged.values()).stream()
                .limit(limit)
                .toList();
    }
}
