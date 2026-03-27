package com.skillsync.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mentor_skills", schema = "mentors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private MentorProfile mentor;

    @Column(nullable = false)
    private Long skillId;
}
