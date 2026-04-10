package com.skillsync.user.repository;

import com.skillsync.user.entity.LearningGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<LearningGroup, Long> {

	@Query("SELECT DISTINCT g FROM LearningGroup g WHERE " +
		   "(:search IS NULL OR :search = '' OR LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(g.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
		   "(:category IS NULL OR :category = '' OR LOWER(g.category) = LOWER(:category)) " +
		   "ORDER BY g.createdAt DESC")
	Page<LearningGroup> searchGroups(@Param("search") String search, @Param("category") String category, Pageable pageable);

	@Query("SELECT DISTINCT g FROM LearningGroup g JOIN g.members m WHERE m.userId = :userId ORDER BY g.createdAt DESC")
	Page<LearningGroup> findMyGroups(@Param("userId") Long userId, Pageable pageable);
}
