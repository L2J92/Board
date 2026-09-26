package com.example.board.auth.presentation;

import com.example.board.auth.application.AuthService;
import com.example.board.auth.presentation.dto.LoginRequest;
import com.example.board.auth.presentation.dto.LoginResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "auth", description = "로그인/로그아웃 등 인증 관리 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String accessToken = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(accessToken));
    }

    @PostMapping("/logout")
    public void logout() {
    }
}
