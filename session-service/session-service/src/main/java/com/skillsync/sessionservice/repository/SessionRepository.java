package com.skillsync.sessionservice.repository;

import com.skillsync.sessionservice.entity.Session;
import com.skillsync.sessionservice.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByLearnerId(Long learnerId);

    List<Session> findByMentorId(Long mentorId);

    List<Session> findByLearnerIdOrMentorId(Long learnerId, Long mentorId);

    List<Session> findByStatus(SessionStatus status);

    boolean existsByMentorIdAndLearnerId(Long mentorId, Long learnerId);
}
