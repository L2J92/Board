package com.example.board.comment.repository;

import com.example.board.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndDeletedFalseAndPost_DeletedFalse(Long id);
    List<Comment> findAllByPost_IdOrderByCreatedAtAscIdAsc(Long postId);
}
