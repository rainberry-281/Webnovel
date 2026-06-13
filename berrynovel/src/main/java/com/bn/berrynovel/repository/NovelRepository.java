package com.bn.berrynovel.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.NovelHot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long>, JpaSpecificationExecutor<Novel> {

  Page<Novel> findAll(Pageable pageable);

  Page<Novel> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

  Page<Novel> findByStatus(boolean status, Pageable pageable);

  boolean existsByTitle(String title);

  boolean existsByTitleAndIdNot(String title, Long id);

  List<Novel> findByStatus(boolean status);

  long countByHot(NovelHot hot);

  Page<Novel> findByHot(NovelHot hot, Pageable pageable);

  Page<Novel> findByHotAndTitleContainingIgnoreCase(NovelHot hot, String keyword, Pageable pageable);

  @Modifying
  @Query("UPDATE Novel n SET n.totalReadCount = COALESCE(n.totalReadCount, 0) + 1 WHERE n.id = :novelId")
  void incrementTotalReadCount(@Param("novelId") Long novelId);

  @Modifying
  @Query("""
      UPDATE Novel n
      SET n.hot = :hot
      WHERE n.id = :novelId
        AND (n.hot IS NULL OR n.hot <> :hot)
        AND COALESCE(n.totalReadCount, 0) >= :threshold
      """)
  int markHotIfReadThresholdReached(@Param("novelId") Long novelId,
      @Param("threshold") Long threshold,
      @Param("hot") NovelHot hot);

  List<Novel> findByStatusTrueOrderByTotalReadCountDesc(Pageable pageable);

  @Query("""
      SELECT DISTINCT n
      FROM Novel n
      JOIN n.genres g
      WHERE n.status = true
        AND g.status = true
        AND n.id <> :novelId
        AND g.id IN (
            SELECT currentGenre.id
            FROM Novel currentNovel
            JOIN currentNovel.genres currentGenre
            WHERE currentNovel.id = :novelId
              AND currentGenre.status = true
        )
      ORDER BY n.ratingAvg DESC, n.totalReadCount DESC
      """)
  List<Novel> findSimilarNovels(@Param("novelId") Long novelId, Pageable pageable);

  @Query(value = """
      SELECT preferred.genre_id
      FROM (
          SELECT ng.genre_id, SUM(signals.signal_score) AS total_score
          FROM (
              SELECT crl.novel_id, COUNT(*) * 1 AS signal_score
              FROM chapter_read_log crl
              WHERE crl.user_id = :userId
                AND crl.read_at >= :since
              GROUP BY crl.novel_id

              UNION ALL

              SELECT b.novel_id, 3 AS signal_score
              FROM Bookshelf b
              WHERE b.user_id = :userId

              UNION ALL

              SELECT nr.novel_id,
                     CASE
                         WHEN nr.score >= 4 THEN 5
                         WHEN nr.score = 3 THEN 0
                         ELSE -3
                     END AS signal_score
              FROM novel_rating nr
              WHERE nr.user_id = :userId
          ) signals
          JOIN novel_genre ng ON signals.novel_id = ng.novel_id
          GROUP BY ng.genre_id
          HAVING total_score > 0
          ORDER BY total_score DESC
      ) preferred
      """, nativeQuery = true)
  List<Integer> findPreferredGenreIds(@Param("userId") Long userId,
      @Param("since") LocalDateTime since,
      Pageable pageable);

  @Query(value = """
      SELECT n.*
      FROM Novels n
      JOIN novel_genre ng ON n.id = ng.novel_id
      JOIN Genres g ON ng.genre_id = g.id
      WHERE ng.genre_id IN (:genreIds)
        AND n.status = true
        AND g.status = true
        AND n.id NOT IN (
            SELECT DISTINCT crl.novel_id
            FROM chapter_read_log crl
            WHERE crl.user_id = :userId
        )
        AND n.id NOT IN (
            SELECT b.novel_id
            FROM Bookshelf b
            WHERE b.user_id = :userId
        )
        AND n.id NOT IN (
            SELECT nr.novel_id
            FROM novel_rating nr
            WHERE nr.user_id = :userId
              AND nr.score <= 2
        )
      GROUP BY n.id
      ORDER BY COUNT(ng.genre_id) DESC,
               CASE
                   WHEN COALESCE(n.rating_count, 0) >= 5
                   THEN (COALESCE(n.rating_avg, 0) * n.rating_count + 3.0 * 10) / (n.rating_count + 10)
                   ELSE 3.0
               END DESC,
               COALESCE(n.total_read_count, 0) DESC
      """, nativeQuery = true)
  List<Novel> findRecommendedByPreferredGenres(@Param("userId") Long userId,
      @Param("genreIds") List<Integer> genreIds,
      Pageable pageable);

  @Query(value = """
      SELECT n.*
      FROM Novels n
      LEFT JOIN (
          SELECT crl.novel_id, COUNT(*) AS recent_reads
          FROM chapter_read_log crl
          WHERE crl.read_at >= :since
          GROUP BY crl.novel_id
      ) recent ON recent.novel_id = n.id
      WHERE n.status = true
        AND (COALESCE(n.rating_count, 0) < 5 OR COALESCE(n.rating_avg, 0) >= 3.5)
      ORDER BY COALESCE(recent.recent_reads, 0) DESC,
               CASE
                   WHEN COALESCE(n.rating_count, 0) >= 5
                   THEN (COALESCE(n.rating_avg, 0) * n.rating_count + 3.0 * 10) / (n.rating_count + 10)
                   ELSE 3.0
               END DESC,
               COALESCE(n.total_read_count, 0) DESC
      """, nativeQuery = true)
  List<Novel> findPopularRecommendations(@Param("since") LocalDateTime since, Pageable pageable);

  @Query(value = """
      SELECT n.*
      FROM Novels n
      WHERE n.status = true
        AND COALESCE(n.rating_count, 0) >= :minimumRatings
      ORDER BY (COALESCE(n.rating_avg, 0) * n.rating_count + 3.0 * 10) / (n.rating_count + 10) DESC,
               COALESCE(n.total_read_count, 0) DESC
      """, nativeQuery = true)
  List<Novel> findTopRatedRecommendations(@Param("minimumRatings") int minimumRatings, Pageable pageable);
}
