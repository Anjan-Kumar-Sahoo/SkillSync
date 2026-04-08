package com.skillsync.user.repository;

import com.skillsync.user.entity.Discussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    Page<Discussion> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    long countByParentId(Long parentId);
}
