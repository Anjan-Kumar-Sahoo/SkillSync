package com.skillsync.user.repository;

import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.enums.MentorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long>, JpaSpecificationExecutor<MentorProfile> {
    Optional<MentorProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Page<MentorProfile> findByStatus(MentorStatus status, Pageable pageable);
    long countByStatus(MentorStatus status);
}
