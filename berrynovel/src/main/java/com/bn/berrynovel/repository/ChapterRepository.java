package com.bn.berrynovel.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bn.berrynovel.domain.Chapter;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Chapter findByTitle(String title);

    List<Chapter> findByNovelId(Long novelId);
}
