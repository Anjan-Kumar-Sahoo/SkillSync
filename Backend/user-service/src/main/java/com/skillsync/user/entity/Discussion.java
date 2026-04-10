package com.skillsync.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity @Table(name = "discussions", schema = "groups",
    indexes = {
        @Index(name = "idx_discussions_group_created_at", columnList = "group_id, created_at"),
        @Index(name = "idx_discussions_parent_id", columnList = "parent_id")
    })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Discussion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id") private LearningGroup group;
    @Column(nullable = false) private Long authorId;
    @Column(length = 150) private String title;
    @Column(nullable = false, length = 5000) private String content;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Discussion parent;
    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;
}
