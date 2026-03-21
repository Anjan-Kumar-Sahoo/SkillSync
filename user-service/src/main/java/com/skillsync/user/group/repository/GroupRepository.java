package com.skillsync.user.group.repository;

import com.skillsync.user.group.entity.LearningGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<LearningGroup, Long> {}
