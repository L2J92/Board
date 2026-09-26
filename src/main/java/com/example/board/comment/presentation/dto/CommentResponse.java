package com.example.board.comment.presentation.dto;

import com.example.board.comment.domain.Comment;

import java.util.List;

public record CommentResponse(
        Long id,
        String content,
        Long parentId,
        Long postId,
        String nickname,
        boolean deleted,
        List<ReplyResponse> replies

) {
    public static CommentResponse from(Comment comment, List<Comment> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getPost().getId(),
                comment.isDeleted() ? null : comment.getWriter().getNickname(),
                comment.isDeleted(),
                replies.stream()
                        .map(ReplyResponse::from)
                        .toList()
        );
    }

    public record ReplyResponse(
            Long id,
            String content,
            Long writerId,
            String nickname,
            boolean deleted
    ) {
        public static ReplyResponse from(Comment reply) {
            return new ReplyResponse(
                    reply.getId(),
                    reply.isDeleted() ? "삭제된 댓글입니다." : reply.getContent(),
                    reply.isDeleted() ? null : reply.getWriter().getId(),
                    reply.isDeleted() ? null : reply.getWriter().getNickname(),
                    reply.isDeleted()
            );
        }
    }
}
