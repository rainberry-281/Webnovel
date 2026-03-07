package com.bn.berrynovel.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bn.berrynovel.domain.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Integer> {

    Page<Novel> findAll(Pageable pageable);

    boolean existsByTitle(String title);

    List<Novel> findByStatus(boolean status);
}
