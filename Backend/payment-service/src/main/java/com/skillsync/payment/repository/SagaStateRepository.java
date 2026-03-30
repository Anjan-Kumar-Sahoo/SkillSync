package com.skillsync.payment.repository;

import com.skillsync.payment.entity.SagaState;
import com.skillsync.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {

    Optional<SagaState> findByPaymentId(Long paymentId);

    /** Find incomplete sagas for recovery on startup */
    List<SagaState> findByStateIn(List<PaymentStatus> states);

    /**
     * Find sagas stuck in a specific state longer than the timeout threshold.
     * Used by SagaRecoveryScheduler to detect and recover stale sagas.
     */
    @Query("SELECT s FROM SagaState s WHERE s.state = :state AND s.lastUpdated < :threshold")
    List<SagaState> findStaleSagas(@Param("state") PaymentStatus state,
                                    @Param("threshold") LocalDateTime threshold);
}
