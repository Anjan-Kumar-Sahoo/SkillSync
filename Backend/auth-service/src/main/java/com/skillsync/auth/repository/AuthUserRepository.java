package com.skillsync.auth.repository;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);
    long countByRole(Role role);
}
