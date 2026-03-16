package com.bn.berrynovel.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.bn.berrynovel.domain.Comment;
import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.repository.CommentRepository;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final NovelService novelService;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository, NovelService novelService, UserService userService) {
        this.commentRepository = commentRepository;
        this.novelService = novelService;
        this.userService = userService;
    }

    public List<Comment> getCommentsByNovelId(Long novelId) {
        return commentRepository.findByNovel_IdOrderByCreatedAtDesc(novelId);
    }

    public Comment createComment(Long novelId, String username, String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        User user = this.userService.getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Novel novel = this.novelService.getNovelById(novelId)
                .orElseThrow(() -> new RuntimeException("Novel not found"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setNovel(novel);
        comment.setContent(content.trim());

        return this.commentRepository.save(comment);
    }

    public boolean deleteComment(Long novelId, Integer commentId, String username, boolean isAdmin) {
        Comment comment = this.commentRepository.findById(commentId)
                .orElse(null);
        if (comment == null || comment.getNovel() == null || comment.getNovel().getId() == null
                || !comment.getNovel().getId().equals(novelId)) {
            return false;
        }

        if (isAdmin) {
            this.commentRepository.delete(comment);
            return true;
        }

        User user = this.userService.getUserByUsername(username);
        if (user == null || comment.getUser() == null || comment.getUser().getId() == null
                || !comment.getUser().getId().equals(user.getId())) {
            return false;
        }

        this.commentRepository.delete(comment);
        return true;
    }
}
