package com.skillsync.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_skills", schema = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long skillId;

    @Enumerated(EnumType.STRING)
    private Proficiency proficiency;

    public enum Proficiency {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
