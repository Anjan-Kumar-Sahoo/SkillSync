package com.skillsync.auth.repository;

import com.skillsync.auth.entity.OtpToken;
import com.skillsync.auth.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId, LocalDateTime now);

    Optional<OtpToken> findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, OtpType type, LocalDateTime now);

    void deleteByEmailAndType(String email, OtpType type);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
