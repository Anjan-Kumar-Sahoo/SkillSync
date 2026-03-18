package com.skillsync.auth.repository;

import com.skillsync.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId, LocalDateTime now);

    Optional<OtpToken> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
