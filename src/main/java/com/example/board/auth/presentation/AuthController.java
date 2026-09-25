package com.example.board.auth.presentation;

import com.example.board.auth.application.AuthService;
import com.example.board.auth.presentation.dto.LoginRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "auth", description = "로그인/로그아웃 등 인증 관리 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public void login(@RequestBody LoginRequest request) {
        authService.login(request.email(), request.password());
    }

    @PostMapping("/logout")
    public void logout() {
    }
}
