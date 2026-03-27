package com.skillsync.auth.repository;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserOrderByCreatedAtAsc(AuthUser user);
    void deleteByUser(AuthUser user);
}
