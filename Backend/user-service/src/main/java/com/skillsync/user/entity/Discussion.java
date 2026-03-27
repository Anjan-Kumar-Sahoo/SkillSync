package com.skillsync.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity @Table(name = "discussions", schema = "groups")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Discussion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id") private LearningGroup group;
    @Column(nullable = false) private Long authorId;
    @Column(nullable = false, length = 5000) private String content;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Discussion parent;
    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
}
