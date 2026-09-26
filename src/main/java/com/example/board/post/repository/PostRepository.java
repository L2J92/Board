package com.example.board.post.repository;

import com.example.board.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByIdAndDeletedFalse(Long id);
    Page<Post> findAllByDeletedFalse(Pageable pageable);
    boolean existsByIdAndDeletedFalse(Long id);
}
