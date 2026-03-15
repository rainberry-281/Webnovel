package com.bn.berrynovel.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bn.berrynovel.domain.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByNovel_IdOrderByCreatedAtDesc(Long novelId);
}
