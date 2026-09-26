package com.example.board.global.security;

import com.example.board.global.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 토큰이 없으면 인증 없이 진행.
        // 공개 경로는 허용하고 보호된 경로는 Security가 차단한다.
        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            unauthorized(response);
            return;
        }

        String token = header.substring(7).trim();

        Long memberId;
        try {
            memberId = jwtTokenProvider.getMemberId(token);
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response);
            return;
        }

        // 검증된 회원 ID를 principal에 저장
        var authentication =
                new UsernamePasswordAuthenticationToken(
                        memberId,
                        null,
                        List.of()
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }

    private void unauthorized(
            HttpServletResponse response
    ) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse(
                        "UNAUTHORIZED",
                        "유효한 인증 토큰이 필요합니다."
                )
        );
    }
}