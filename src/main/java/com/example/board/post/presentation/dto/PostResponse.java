package com.example.board.post.presentation.dto;

import com.example.board.post.domain.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        String writer,
        LocalDateTime createAt,
        LocalDateTime updateAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getWriter().getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
