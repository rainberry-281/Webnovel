package com.bn.berrynovel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.NovelRating;
import com.bn.berrynovel.repository.NovelRatingRepository;
import com.bn.berrynovel.repository.NovelRepository;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private NovelRatingRepository ratingRepository;

    @Mock
    private NovelRepository novelRepository;

    @Test
    void firstRatingCreatesVoteAndRefreshesNovelCache() {
        RatingService ratingService = new RatingService(this.ratingRepository, this.novelRepository);
        Novel novel = new Novel();
        novel.setId(5L);

        when(this.novelRepository.findById(5L)).thenReturn(Optional.of(novel));
        when(this.ratingRepository.findByNovelIdAndUserId(5L, 9L)).thenReturn(Optional.empty());
        when(this.ratingRepository.findAverageScoreByNovelId(5L)).thenReturn(4.24);
        when(this.ratingRepository.countByNovelId(5L)).thenReturn(12L);
        when(this.novelRepository.save(novel)).thenReturn(novel);

        Novel result = ratingService.rate(5L, 9L, 4);

        ArgumentCaptor<NovelRating> ratingCaptor = ArgumentCaptor.forClass(NovelRating.class);
        verify(this.ratingRepository).save(ratingCaptor.capture());
        NovelRating savedRating = ratingCaptor.getValue();

        assertThat(savedRating.getNovelId()).isEqualTo(5L);
        assertThat(savedRating.getUserId()).isEqualTo(9L);
        assertThat(savedRating.getScore()).isEqualTo(4);
        assertThat(result.getRatingAvg()).isEqualTo(4.2);
        assertThat(result.getRatingCount()).isEqualTo(12);
        verify(this.novelRepository).save(novel);
    }

    @Test
    void existingRatingUpdatesSameVote() {
        RatingService ratingService = new RatingService(this.ratingRepository, this.novelRepository);
        Novel novel = new Novel();
        novel.setId(5L);
        NovelRating existingRating = new NovelRating();
        existingRating.setNovelId(5L);
        existingRating.setUserId(9L);
        existingRating.setScore(2);

        when(this.novelRepository.findById(5L)).thenReturn(Optional.of(novel));
        when(this.ratingRepository.findByNovelIdAndUserId(5L, 9L)).thenReturn(Optional.of(existingRating));
        when(this.ratingRepository.findAverageScoreByNovelId(5L)).thenReturn(3.95);
        when(this.ratingRepository.countByNovelId(5L)).thenReturn(7L);
        when(this.novelRepository.save(novel)).thenReturn(novel);

        Novel result = ratingService.rate(5L, 9L, 5);

        assertThat(existingRating.getScore()).isEqualTo(5);
        assertThat(result.getRatingAvg()).isEqualTo(4.0);
        assertThat(result.getRatingCount()).isEqualTo(7);
        verify(this.ratingRepository).save(existingRating);
    }

    @Test
    void invalidScoreIsRejectedBeforeDatabaseWork() {
        RatingService ratingService = new RatingService(this.ratingRepository, this.novelRepository);

        assertThatThrownBy(() -> ratingService.rate(5L, 9L, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Score must be between 1 and 5");

        verify(this.novelRepository, never()).findById(any());
        verify(this.ratingRepository, never()).save(any());
    }
}
