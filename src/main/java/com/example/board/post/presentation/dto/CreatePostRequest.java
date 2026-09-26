package com.example.board.post.presentation.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank
        @Size(min = 1, max = 100)
        String title,

        @NotBlank
        String content
) {
}
