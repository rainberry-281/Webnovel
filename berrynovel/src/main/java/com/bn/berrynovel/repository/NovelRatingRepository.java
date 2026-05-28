package com.bn.berrynovel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bn.berrynovel.domain.NovelRating;

@Repository
public interface NovelRatingRepository extends JpaRepository<NovelRating, Long> {

    Optional<NovelRating> findByNovelIdAndUserId(Long novelId, Long userId);

    @Query("SELECT AVG(r.score) FROM NovelRating r WHERE r.novelId = :novelId")
    Double findAverageScoreByNovelId(@Param("novelId") Long novelId);

    long countByNovelId(Long novelId);
}
