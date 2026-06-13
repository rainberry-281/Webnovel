package com.bn.berrynovel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.repository.NovelRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private NovelRepository novelRepository;

    @Test
    void personalizedRecommendationsUsePreferredGenresWhenAvailable() {
        RecommendationService recommendationService = new RecommendationService(this.novelRepository);
        Novel novel = new Novel();
        novel.setId(10L);
        Novel popularNovel = new Novel();
        popularNovel.setId(20L);

        when(this.novelRepository.findPreferredGenreIds(eq(5L), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(1, 2));
        when(this.novelRepository.findRecommendedByPreferredGenres(eq(5L), eq(List.of(1, 2)), any(Pageable.class)))
                .thenReturn(List.of(novel));
        when(this.novelRepository.findPopularRecommendations(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(novel, popularNovel));

        List<Novel> recommendations = recommendationService.getPersonalizedRecommendations(5L, 6);

        assertThat(recommendations).containsExactly(novel, popularNovel);
        verify(this.novelRepository).findRecommendedByPreferredGenres(eq(5L), eq(List.of(1, 2)), any(Pageable.class));
    }

    @Test
    void personalizedRecommendationsFallbackToPopularWhenNoSignals() {
        RecommendationService recommendationService = new RecommendationService(this.novelRepository);
        Novel fallbackNovel = new Novel();
        fallbackNovel.setId(20L);

        when(this.novelRepository.findPreferredGenreIds(eq(5L), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(this.novelRepository.findPopularRecommendations(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(fallbackNovel));

        List<Novel> recommendations = recommendationService.getPersonalizedRecommendations(5L, 6);

        assertThat(recommendations).containsExactly(fallbackNovel);
        verify(this.novelRepository).findPopularRecommendations(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void similarNovelsDelegatesToRepositoryWithLimit() {
        RecommendationService recommendationService = new RecommendationService(this.novelRepository);
        Novel similarNovel = new Novel();
        similarNovel.setId(30L);

        when(this.novelRepository.findSimilarNovels(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(similarNovel));

        List<Novel> similarNovels = recommendationService.getSimilarNovels(7L, 4);

        assertThat(similarNovels).containsExactly(similarNovel);
        verify(this.novelRepository).findSimilarNovels(eq(7L), any(Pageable.class));
    }

    @Test
    void invalidLimitsReturnEmptyList() {
        RecommendationService recommendationService = new RecommendationService(this.novelRepository);

        assertThat(recommendationService.getPopularNovels(7, 0)).isEmpty();
        assertThat(recommendationService.getTopRatedNovels(5, -1)).isEmpty();
        assertThat(recommendationService.getPersonalizedRecommendations(1L, 0)).isEmpty();
        assertThat(recommendationService.getSimilarNovels(1L, 0)).isEmpty();
    }
}
