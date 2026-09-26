package com.example.board.comment.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank
        String content
) {
}
