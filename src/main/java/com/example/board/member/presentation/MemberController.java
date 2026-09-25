package com.example.board.member.presentation;


import com.example.board.member.application.MemberService;
import com.example.board.member.presentation.dto.JoinMemberRequest;
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
@RequestMapping("/member")
@Tag(name = "Member", description = "")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<Void> join(
            @Valid @RequestBody JoinMemberRequest request
    ) {
        memberService.join(
                request.id(),
                request.password(),
                request.nickName()
        );
        return ResponseEntity.ok().build();
    }

}
