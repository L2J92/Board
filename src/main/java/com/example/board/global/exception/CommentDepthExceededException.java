package com.example.board.global.exception;

public class CommentDepthExceededException extends RuntimeException {
    public CommentDepthExceededException() {
        super("댓글의 댓글까지만 허용됩니다.");
    }
}