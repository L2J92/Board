package com.example.board.comment.repository;

import com.example.board.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPost_IdOrderByCreatedAtAscIdAsc(Long postId);
}
