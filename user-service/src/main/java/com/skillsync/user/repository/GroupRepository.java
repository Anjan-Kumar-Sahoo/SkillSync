package com.skillsync.user.repository;

import com.skillsync.user.entity.LearningGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<LearningGroup, Long> {}
