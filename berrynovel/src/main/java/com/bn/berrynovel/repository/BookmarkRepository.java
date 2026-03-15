package com.bn.berrynovel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bn.berrynovel.domain.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUser_IdAndChapter_Id(Long userId, Long chapterId);

    void deleteByUser_IdAndChapter_Id(Long userId, Long chapterId);

    List<Bookmark> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
