package com.skillsync.group.repository;

import com.skillsync.group.entity.LearningGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<LearningGroup, Long> {}
