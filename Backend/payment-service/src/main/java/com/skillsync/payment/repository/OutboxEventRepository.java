package com.skillsync.payment.repository;

import com.skillsync.payment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Atomically claim PENDING events using PostgreSQL's FOR UPDATE SKIP LOCKED.
     * This guarantees multi-instance safety — only one instance processes each row.
     */
    @Query(value = "SELECT * FROM payments.outbox_events " +
            "WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findAndLockPendingEvents(@Param("limit") int limit);

    /**
     * Atomically claim FAILED events for retry (max retries enforced).
     * FOR UPDATE SKIP LOCKED ensures no duplicate processing.
     */
    @Query(value = "SELECT * FROM payments.outbox_events " +
            "WHERE status = 'FAILED' AND retry_count < :maxRetries " +
            "ORDER BY created_at ASC " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findAndLockFailedEventsForRetry(@Param("maxRetries") int maxRetries,
                                                      @Param("limit") int limit);

    /**
     * Recover stale PROCESSING events (publisher crashed after claiming but before finishing).
     * If lastAttemptAt is older than the threshold, the row is considered abandoned.
     */
    @Query(value = "SELECT * FROM payments.outbox_events " +
            "WHERE status = 'PROCESSING' AND last_attempt_at < :threshold " +
            "ORDER BY created_at ASC " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findStaleProcessingEvents(@Param("threshold") LocalDateTime threshold,
                                                 @Param("limit") int limit);

    /**
     * Cleanup: delete SENT events older than the given cutoff.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'SENT' AND e.processedAt < :cutoff")
    int deleteSentEventsBefore(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByEventId(String eventId);

    Optional<OutboxEvent> findByEventId(String eventId);
}
