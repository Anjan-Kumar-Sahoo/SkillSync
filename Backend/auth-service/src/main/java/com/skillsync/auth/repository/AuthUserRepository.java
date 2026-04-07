package com.skillsync.auth.repository;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);
    long countByRole(Role role);
    Page<AuthUser> findByRole(Role role, Pageable pageable);

    @Query("SELECT u FROM AuthUser u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AuthUser> findByFilters(@Param("role") Role role, @Param("search") String search, Pageable pageable);
}
