package com.example.board.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenExpiration;
    private final JwtParser parser;

    public JwtTokenProvider(JwtProperties properties) {
        // Base64로 저장한 키를 디코딩해 서명 키로 변환
        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.secret())
        );

        this.accessTokenExpiration = properties.accessTokenExpiration();

        if (accessTokenExpiration == null
                || accessTokenExpiration.isZero()
                || accessTokenExpiration.isNegative()) {
            throw new IllegalArgumentException(
                    "JWT 만료 시간은 0보다 커야 합니다."
            );
        }

        // 토큰 검증에 사용할 객체를 한 번 생성
        this.parser = Jwts.parser()
                .verifyWith(key)
                .build();
    }

    // 로그인 성공 후 호출
    public String createAccessToken(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException(
                    "유효한 회원 ID가 필요합니다."
            );
        }

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // 서명·만료 검증에 성공한 경우에만 회원 ID 반환
    public Long getMemberId(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("토큰이 비어 있습니다.");
        }

        Claims claims = parser.parseSignedClaims(token)
                .getPayload();

        // 만료 정보가 없는 토큰도 거절
        if (claims.getExpiration() == null) {
            throw new JwtException("토큰에 만료 시간이 없습니다.");
        }

        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtException("토큰에 회원 ID가 없습니다.");
        }

        try {
            Long memberId = Long.valueOf(subject);

            if (memberId <= 0) {
                throw new JwtException("회원 ID가 올바르지 않습니다.");
            }

            return memberId;
        } catch (NumberFormatException e) {
            throw new JwtException("회원 ID 형식이 올바르지 않습니다.", e);
        }
    }
}