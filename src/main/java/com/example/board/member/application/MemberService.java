package com.example.board.member.application;

import com.example.board.global.exception.DuplicateEmailException;
import com.example.board.member.domain.Member;
import com.example.board.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long join(String email, String password, String nickname) {
        if(memberRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        String encodedPassword = passwordEncoder.encode(password);

        Member member = Member.create(email, encodedPassword, nickname);
        Member savedmember =  memberRepository.save(member);

        return savedmember.getId();
    }

}
