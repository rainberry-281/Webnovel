package com.bn.berrynovel.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bn.berrynovel.domain.ChapterReadLog;

@Repository
public interface ChapterReadLogRepository extends JpaRepository<ChapterReadLog, Long> {

    Optional<ChapterReadLog> findFirstByChapterIdAndUserIdAndReadAtAfter(
            Long chapterId, Long userId, LocalDateTime since);

    Optional<ChapterReadLog> findFirstByChapterIdAndSessionIdAndReadAtAfter(
            Long chapterId, String sessionId, LocalDateTime since);

    void deleteByReadAtBefore(LocalDateTime cutoff);

    @Query("SELECT COUNT(l) FROM ChapterReadLog l WHERE l.novelId = :novelId AND l.readAt >= :since")
    long countByNovelIdAndReadAtAfter(Long novelId, LocalDateTime since);
}
