package com.skillsync.group.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity @Table(name = "group_members", schema = "groups",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GroupMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id") private LearningGroup group;
    @Column(nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) private MemberRole role;
    @CreatedDate @Column(updatable = false) private LocalDateTime joinedAt;

    public enum MemberRole { OWNER, ADMIN, MEMBER }
}
