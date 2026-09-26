package com.example.board.deletion;

import com.example.board.comment.application.CommentService;
import com.example.board.comment.domain.Comment;
import com.example.board.comment.repository.CommentRepository;
import com.example.board.global.exception.NotFoundException;
import com.example.board.member.domain.Member;
import com.example.board.member.repository.MemberRepository;
import com.example.board.post.application.PostService;
import com.example.board.post.domain.Post;
import com.example.board.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({PostService.class, CommentService.class})
class SoftDeleteTests {
    @Autowired PostService posts;
    @Autowired CommentService comments;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired TestEntityManager entityManager;

    private Long memberId;
    private Long postId;
    private Long parentId;
    private Long replyId;

    @BeforeEach
    void setUp() {
        Member writer = memberRepository.save(Member.create("writer@example.com", "hash", "writer"));
        Post post = postRepository.save(Post.create("title", "content", writer));
        Comment parent = commentRepository.save(Comment.create("parent secret", post, writer));
        Comment reply = commentRepository.save(Comment.create("reply secret", post, writer, parent));
        memberId = writer.getId();
        postId = post.getId();
        parentId = parent.getId();
        replyId = reply.getId();
        flushAndClear();
    }

    @Test
    void deletedPostRemainsStoredButCannotBeReadOrChanged() {
        posts.deletePost(memberId, postId);
        flushAndClear();

        assertThat(postRepository.findById(postId).orElseThrow().isDeleted()).isTrue();
        assertThat(commentRepository.count()).isEqualTo(2);
        assertThat(posts.getPosts(PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThatThrownBy(() -> posts.getPost(postId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> posts.updatePost(memberId, postId, "changed", "changed"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> posts.deletePost(memberId, postId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.getComments(postId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.createComment(memberId, postId, "new"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.createComment(memberId, postId, parentId, "new reply"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.updateComments(memberId, replyId, "changed"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.deleteComments(memberId, replyId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletedParentIsMaskedAndKeepsItsReply() {
        comments.deleteComments(memberId, parentId);
        flushAndClear();

        assertThat(commentRepository.count()).isEqualTo(2);
        assertThat(commentRepository.findById(parentId).orElseThrow().isDeleted()).isTrue();
        var result = comments.getComments(postId);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().deleted()).isTrue();
        assertThat(result.getFirst().content()).isEqualTo("삭제된 댓글입니다.");
        assertThat(result.getFirst().nickname()).isNull();
        assertThat(result.getFirst().replies()).hasSize(1);
        assertThat(result.getFirst().replies().getFirst().content()).isEqualTo("reply secret");
        assertThatThrownBy(() -> comments.updateComments(memberId, parentId, "restore"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.createComment(memberId, postId, parentId, "new reply"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> comments.deleteComments(memberId, parentId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void existingReplyCanBeUpdatedAndSoftDeletedAfterParentDeletion() {
        comments.deleteComments(memberId, parentId);
        comments.updateComments(memberId, replyId, "edited reply");
        flushAndClear();
        assertThat(comments.getComments(postId).getFirst().replies().getFirst().content())
                .isEqualTo("edited reply");

        comments.deleteComments(memberId, replyId);
        flushAndClear();
        var reply = comments.getComments(postId).getFirst().replies().getFirst();
        assertThat(reply.deleted()).isTrue();
        assertThat(reply.content()).isEqualTo("삭제된 댓글입니다.");
        assertThat(reply.writerId()).isNull();
        assertThat(reply.nickname()).isNull();
        assertThat(commentRepository.findById(replyId).orElseThrow().isDeleted()).isTrue();
        assertThatThrownBy(() -> comments.updateComments(memberId, replyId, "restore"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void anotherMemberCannotDeletePostOrComment() {
        Long otherId = memberRepository.save(Member.create("other@example.com", "hash", "other")).getId();
        assertThatThrownBy(() -> posts.deletePost(otherId, postId)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> comments.deleteComments(otherId, parentId)).isInstanceOf(AccessDeniedException.class);
        flushAndClear();
        assertThat(postRepository.findById(postId).orElseThrow().isDeleted()).isFalse();
        assertThat(commentRepository.findById(parentId).orElseThrow().isDeleted()).isFalse();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
