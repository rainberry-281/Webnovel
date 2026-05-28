package com.bn.berrynovel.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.NovelRating;
import com.bn.berrynovel.repository.NovelRatingRepository;
import com.bn.berrynovel.repository.NovelRepository;

@Service
public class RatingService {

    private final NovelRatingRepository ratingRepository;
    private final NovelRepository novelRepository;

    public RatingService(NovelRatingRepository ratingRepository, NovelRepository novelRepository) {
        this.ratingRepository = ratingRepository;
        this.novelRepository = novelRepository;
    }

    @Transactional
    public Novel rate(Long novelId, Long userId, int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }

        Novel novel = this.novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("Novel not found"));

        NovelRating rating = this.ratingRepository.findByNovelIdAndUserId(novelId, userId)
                .orElseGet(() -> {
                    NovelRating newRating = new NovelRating();
                    newRating.setNovelId(novelId);
                    newRating.setUserId(userId);
                    return newRating;
                });

        rating.setScore(score);
        this.ratingRepository.save(rating);

        return refreshNovelRatingCache(novel);
    }

    public Optional<Integer> getUserRating(Long novelId, Long userId) {
        return this.ratingRepository.findByNovelIdAndUserId(novelId, userId)
                .map(NovelRating::getScore);
    }

    private Novel refreshNovelRatingCache(Novel novel) {
        Double averageScore = this.ratingRepository.findAverageScoreByNovelId(novel.getId());
        long ratingCount = this.ratingRepository.countByNovelId(novel.getId());

        double roundedAverage = averageScore == null ? 0.0 : Math.round(averageScore * 10.0) / 10.0;
        novel.setRatingAvg(roundedAverage);
        novel.setRatingCount((int) ratingCount);

        return this.novelRepository.save(novel);
    }
}
