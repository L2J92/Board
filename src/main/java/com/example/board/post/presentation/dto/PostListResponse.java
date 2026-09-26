package com.example.board.post.presentation.dto;

import com.example.board.post.domain.Post;

import java.time.LocalDateTime;

public record PostListResponse(
        Long id,
        String title,
        String writer,
        long commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostListResponse from(Post post, long commentCount) {
        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getWriter().getNickname(),
                commentCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}