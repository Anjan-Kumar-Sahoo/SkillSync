package com.skillsync.session.entity;

import com.skillsync.session.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity @Table(name = "sessions", schema = "sessions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Session {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long mentorId;
    @Column(nullable = false) private Long learnerId;
    @Column(nullable = false) private String topic;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private LocalDateTime sessionDate;
    private int durationMinutes;
    private String meetingLink;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SessionStatus status;
    private String cancelReason;
    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public boolean isTransitionAllowed(SessionStatus target) {
        return this.status.canTransitionTo(target);
    }
}
