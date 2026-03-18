package com.skillsync.sessionservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "learner_id", nullable = false)
    private Long learnerId;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;

    @Column(name = "topic")
    private String topic;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMentorId() {
		return mentorId;
	}

	public void setMentorId(Long mentorId) {
		this.mentorId = mentorId;
	}

	public Long getLearnerId() {
		return learnerId;
	}

	public void setLearnerId(Long learnerId) {
		this.learnerId = learnerId;
	}

	public LocalDateTime getSessionDate() {
		return sessionDate;
	}

	public void setSessionDate(LocalDateTime sessionDate) {
		this.sessionDate = sessionDate;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public void setStatus(SessionStatus status) {
		this.status = status;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Session(Long id, Long mentorId, Long learnerId, LocalDateTime sessionDate, SessionStatus status,
			String topic, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.mentorId = mentorId;
		this.learnerId = learnerId;
		this.sessionDate = sessionDate;
		this.status = status;
		this.topic = topic;
		this.createdAt = createdAt;
	}
    
    
}
