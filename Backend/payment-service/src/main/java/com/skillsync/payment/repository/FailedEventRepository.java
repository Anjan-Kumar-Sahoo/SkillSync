package com.skillsync.payment.repository;

import com.skillsync.payment.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    Optional<FailedEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<FailedEvent> findByReplayStatus(FailedEvent.ReplayStatus status);

    long countByReplayStatus(FailedEvent.ReplayStatus status);
}
