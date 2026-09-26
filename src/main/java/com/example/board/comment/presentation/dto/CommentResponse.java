package com.example.board.comment.presentation.dto;

import com.example.board.comment.domain.Comment;

import java.util.List;

public record CommentResponse(
        Long id,
        String content,
        Long parentId,
        Long postId,
        String nickname,
        List<ReplyResponse> replies

) {
    public static CommentResponse from(Comment comment, List<Comment> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getPost().getId(),
                comment.getWriter().getNickname(),
                replies.stream()
                        .map(ReplyResponse::from)
                        .toList()
        );
    }

    public record ReplyResponse(
            Long id,
            String content,
            Long writerId,
            String nickname
    ) {
        public static ReplyResponse from(Comment reply) {
            return new ReplyResponse(
                    reply.getId(),
                    reply.getContent(),
                    reply.getWriter().getId(),
                    reply.getWriter().getNickname()
            );
        }
    }
}
