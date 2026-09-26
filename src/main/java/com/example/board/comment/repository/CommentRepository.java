package com.example.board.comment.repository;

import com.example.board.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndDeletedFalseAndPost_DeletedFalse(Long id);
    List<Comment> findAllByPost_IdOrderByCreatedAtAscIdAsc(Long postId);

    @Query("""
        select c.post.id as postId, count(c) as commentCount
        from Comment c
        where c.post.id in :postIds
          and c.deleted = false
        group by c.post.id
        """)
    List<PostCommentCount> countCommentsByPostIds(
            @Param("postIds") List<Long> postIds
    );
}
