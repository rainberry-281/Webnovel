package com.bn.berrynovel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bn.berrynovel.domain.Bookshelf;
import com.bn.berrynovel.domain.BookshelfId;

@Repository
public interface BookshelfRepository extends JpaRepository<Bookshelf, BookshelfId> {
    boolean existsByUser_IdAndNovel_Id(Long userId, Long novelId);

    void deleteByUser_IdAndNovel_Id(Long userId, Long novelId);

    List<Bookshelf> findByUser_IdOrderByNovel_TitleAsc(Long userId);

    void deleteByUser_IdAndNovel_IdIn(Long userId, List<Long> novelIds);
}
