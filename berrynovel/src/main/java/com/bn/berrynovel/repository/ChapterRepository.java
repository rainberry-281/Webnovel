package com.bn.berrynovel.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bn.berrynovel.domain.Chapter;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Chapter findByTitle(String title);

    @Query(value = "select * from chapters c where c.novel_id = :novelId order by c.id asc limit 1", nativeQuery = true)
    Chapter findFirstChapter(Long novelId);

    @Query(value = "select * from chapters c where c.novel_id = :novelId order by c.id desc limit 1", nativeQuery = true)
    Chapter findLastChapter(Long novelId);

    List<Chapter> findByNovelId(Long novelId);
}
