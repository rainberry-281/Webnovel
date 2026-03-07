package com.bn.berrynovel.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bn.berrynovel.domain.Genre;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    Genre findByName(String name);

    Genre findByCode(String code);

    List<Genre> findByStatus(boolean status);
}
