package com.example.board.comment.application;


import com.example.board.comment.domain.Comment;
import com.example.board.comment.presentation.dto.CommentResponse;
import com.example.board.comment.repository.CommentRepository;
import com.example.board.global.exception.NotFoundException;
import com.example.board.member.domain.Member;
import com.example.board.member.repository.MemberRepository;
import com.example.board.post.domain.Post;
import com.example.board.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createComment(Long memberId, Long postId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found"));

        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() ->
                new NotFoundException("post not found"));

        commentRepository.save(Comment.create(content, post, member));

    }

    @Transactional
    public void createComment(Long memberId, Long postId, Long commentId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found"));

        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() ->
                new NotFoundException("post not found"));

        Comment comment = commentRepository.findByIdAndDeletedFalseAndPost_DeletedFalse(commentId).orElseThrow(() ->
                new NotFoundException("comment not found"));

        if(!comment.getPost().getId().equals(postId)) {
            throw new NotFoundException("access denied");
        }

        commentRepository.save(Comment.create(content, post, member, comment));

    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        if (!postRepository.existsByIdAndDeletedFalse(postId)) {
            throw new NotFoundException("post not found");
        }
        List<Comment> comments =
                commentRepository.findAllByPost_IdOrderByCreatedAtAscIdAsc(postId);

        return comments.stream()
                .filter(comment -> comment.getParent() == null)
                .map(parent -> CommentResponse.from(
                        parent,
                        findReplies(comments, parent.getId())
                ))
                .toList();
    }

    private List<Comment> findReplies(List<Comment> comments, Long parentId) {
        return comments.stream()
                .filter(comment -> comment.getParent() != null)
                .filter(comment ->
                        comment.getParent().getId().equals(parentId)
                )
                .toList();
    }

    @Transactional
    public void updateComments(Long memberId, Long commentId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found"));

        Comment comment = commentRepository.findByIdAndDeletedFalseAndPost_DeletedFalse(commentId).orElseThrow(() ->
                new NotFoundException("comment not found"));

        if (!comment.getWriter().getId().equals(member.getId())) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }

        comment.update(content);

    }

    @Transactional
    public void deleteComments(Long memberId, Long commentId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found"));

        Comment comment = commentRepository.findByIdAndDeletedFalseAndPost_DeletedFalse(commentId).orElseThrow(() ->
                new NotFoundException("comment not found"));

        if (!comment.getWriter().getId().equals(member.getId())) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }

        comment.delete();
    }
}
