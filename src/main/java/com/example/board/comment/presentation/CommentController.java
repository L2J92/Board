package com.example.board.comment.presentation;

import com.example.board.comment.application.CommentService;
import com.example.board.comment.presentation.dto.CommentResponse;
import com.example.board.comment.presentation.dto.CreateCommentRequest;
import com.example.board.comment.presentation.dto.UpdateCommentRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}")
    public ResponseEntity<Void> createComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request

    ) {
        commentService.createComment(memberId, postId, request.content());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/{commentId}")
    public ResponseEntity<Void> createComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request

    ) {
        commentService.createComment(memberId, postId, commentId, request.content());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{postId}")
    public List<CommentResponse> getComment(
            @PathVariable Long postId
    ) {
        return commentService.getComments(postId);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        commentService.updateComments(memberId, commentId, request.content());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComments(memberId, commentId);
        return ResponseEntity.ok().build();
    }

}
