package com.example.board.auth.application;

import com.example.board.global.security.JwtTokenProvider;
import com.example.board.member.domain.Member;
import com.example.board.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    @Transactional
    public String login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->
                        new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.")
                        );

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return jwtTokenProvider.createAccessToken(member.getId());
    }

}
