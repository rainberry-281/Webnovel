package com.bn.berrynovel.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.NovelHot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

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
}
