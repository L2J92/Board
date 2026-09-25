package com.example.board.global.exception;

public record ErrorResponse(
        String code,
        String message
) {

}
