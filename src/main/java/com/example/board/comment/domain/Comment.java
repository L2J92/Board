package com.example.board.comment.domain;

import com.example.board.member.domain.Member;
import com.example.board.post.domain.Post;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    public Comment(String content, Post post, Member writer) {
        this.content = content;
        this.post = post;
        this.writer = writer;
    }

    public Comment(String content, Post post, Member writer, Comment parent) {
        this.content = content;
        this.post = post;
        this.writer = writer;
        this.parent = parent;
    }

    public static Comment create(String content,Post post, Member writer) {
        return new Comment(content, post ,writer);
    }

    public static Comment create(String content,Post post, Member writer, Comment parent) {
        if(parent.getParent() != null) throw new DuplicateRequestException("댓글의 댓글까지만 허용됩니다.");
        return new Comment(content, post ,writer, parent);
    }

    public void delete() {
        this.deleted = true;
    }

    public void update(String content) {
        this.content = content;
    }
}
