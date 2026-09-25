package com.example.board.member.presentation;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@NoArgsConstructor
@RequestMapping("/member")
@Tag(name = "Member", description = "")
public class MemberController {


}
