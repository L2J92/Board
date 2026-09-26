package com.example.board.post.application;

import com.example.board.global.exception.NotFoundException;
import com.example.board.member.domain.Member;
import com.example.board.member.repository.MemberRepository;
import com.example.board.post.domain.Post;
import com.example.board.post.presentation.dto.PostResponse;
import com.example.board.post.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createPost(Long memberId, String title, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found")
        );
        postRepository.save(Post.create(title, content, member));
    }

    @Transactional
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() ->
                new NotFoundException("post not found")
        );

        return PostResponse.from(post);
    }

    @Transactional
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(PostResponse::from);
    }

    @Transactional
    public void updatePost(Long memberId, Long postId, String title, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException("member not found")
        );

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new NotFoundException("post not found")
        );

        if (!post.getWriter().equals(member)) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }

        post.update(title, content);
    }

    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found")
        );

        Post post = postRepository.findById(postId).orElseThrow(() ->
                new NotFoundException("post not found")
        );

        if (!post.getWriter().equals(member)) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }

        postRepository.deleteById(postId);

    }
}
