package com.example.board.post.presentation;


import com.example.board.post.application.PostService;
import com.example.board.post.presentation.dto.CreatePostRequest;
import com.example.board.post.presentation.dto.PostResponse;
import com.example.board.post.presentation.dto.UpdatePostRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
@Tag(name = "Post", description = "게시판 관리 API")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Void> createPost(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreatePostRequest request) {
        postService.createPost(memberId, request.title(), request.content());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    @GetMapping
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postService.getPosts(pageable);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest post) {
        postService.updatePost(memberId, id, post.title(), post.content());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@AuthenticationPrincipal Long memberId, @PathVariable Long id) {
        postService.deletePost(memberId, id);
        return ResponseEntity.ok().build();
    }

}
