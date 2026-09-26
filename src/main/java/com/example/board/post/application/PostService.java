package com.example.board.post.application;

import com.example.board.comment.repository.CommentRepository;
import com.example.board.comment.repository.PostCommentCount;
import com.example.board.global.exception.NotFoundException;
import com.example.board.member.domain.Member;
import com.example.board.member.repository.MemberRepository;
import com.example.board.post.domain.Post;
import com.example.board.post.presentation.dto.PostListResponse;
import com.example.board.post.presentation.dto.PostResponse;
import com.example.board.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void createPost(Long memberId, String title, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found")
        );
        postRepository.save(Post.create(title, content, member));
    }

    @Transactional
    public PostResponse getPost(Long id) {
        Post post = postRepository.findByIdAndDeletedFalse(id).orElseThrow(() ->
                new NotFoundException("post not found")
        );

        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getPosts(Pageable pageable) {
        // 요청한 페이지 번호·크기는 유지하고 정렬은 최신순으로 고정
        Pageable latestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Post> posts = postRepository.findAllByDeletedFalse(latestFirst);

        // 빈 ID 목록으로 집계 쿼리를 실행하지 않음
        if (posts.isEmpty()) {
            return posts.map(post -> PostListResponse.from(post, 0L));
        }

        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .toList();

        Map<Long, Long> commentCounts = commentRepository
                .countCommentsByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostCommentCount::getPostId,
                        PostCommentCount::getCommentCount
                ));

        return posts.map(post -> PostListResponse.from(
                post,
                commentCounts.getOrDefault(post.getId(), 0L)
        ));
    }

    @Transactional
    public void updatePost(Long memberId, Long postId, String title, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException("member not found")
        );

        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(
                () -> new NotFoundException("post not found")
        );

        if (!post.getWriter().getId().equals(member.getId())) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }

        post.update(title, content);
    }

    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new NotFoundException("member not found")
        );

        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() ->
                new NotFoundException("post not found")
        );

        if (!post.getWriter().getId().equals(member.getId())) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }

        post.delete();

    }
}
