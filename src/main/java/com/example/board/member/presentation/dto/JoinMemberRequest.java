package com.example.board.member.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinMemberRequest(
        @NotBlank
        @Email
        String id,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "[\\x21-\\x7E]+",
                message = "비밀번호는 영문, 숫자, ASCII 특수문자만 사용할 수 있습니다."
        )
        String password,

        @NotBlank
        @Size(min = 2, max = 30)
        String nickName
) {
}
