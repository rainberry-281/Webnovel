package com.bn.berrynovel.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bn.berrynovel.domain.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Integer> {

    Page<Novel> findAll(Pageable pageable);

    boolean existsByTitle(String title);

    List<Novel> findByStatus(boolean status);

    @Query(value = "select * from Novels n where n.type = 'ORIGINAL' order by n.id desc limit 6", nativeQuery = true)
    List<Novel> findTop6OriginalNovels(Pageable pageable);
}
