package com.skillsync.session.repository;

import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Page<Session> findByLearnerId(Long learnerId, Pageable pageable);
    Page<Session> findByLearnerIdAndStatusIn(Long learnerId, List<SessionStatus> statuses, Pageable pageable);
    Page<Session> findByMentorId(Long mentorId, Pageable pageable);
    Page<Session> findByMentorIdAndStatusIn(Long mentorId, List<SessionStatus> statuses, Pageable pageable);
    Page<Session> findByMentorIdAndStatus(Long mentorId, SessionStatus status, Pageable pageable);
    List<Session> findByMentorIdAndStatusIn(Long mentorId, List<SessionStatus> statuses);
    Optional<Session> findFirstByMentorIdAndLearnerIdAndStatusOrderBySessionDateDesc(
           Long mentorId,
           Long learnerId,
           SessionStatus status
    );

    boolean existsByMentorIdAndLearnerIdAndSessionDateAndDurationMinutesAndStatusIn(
            Long mentorId,
            Long learnerId,
            LocalDateTime sessionDate,
            int durationMinutes,
            List<SessionStatus> statuses
    );

    boolean existsByMentorIdAndLearnerIdAndSessionDateAndStatusIn(
           Long mentorId,
           Long learnerId,
           LocalDateTime sessionDate,
           List<SessionStatus> statuses
    );

    @Query("SELECT s FROM Session s WHERE s.mentorId = :mentorId AND s.status IN ('REQUESTED','ACCEPTED') " +
           "AND s.sessionDate < :endTime AND FUNCTION('TIMESTAMPADD', MINUTE, s.durationMinutes, s.sessionDate) > :startTime")
    List<Session> findConflictingSessions(@Param("mentorId") Long mentorId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
