package com.skillsync.session.repository;

import com.skillsync.session.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByMentorIdOrderByCreatedAtDesc(Long mentorId, Pageable pageable);
    Page<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId, Pageable pageable);
    Optional<Review> findBySessionId(Long sessionId);
    boolean existsBySessionId(Long sessionId);
    boolean existsByMentorIdAndReviewerId(Long mentorId, Long reviewerId);
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mentorId = :mentorId")
    Double calculateAverageRating(@Param("mentorId") Long mentorId);

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM Review r WHERE r.mentorId = :mentorId")
    Double calculateTotalRating(@Param("mentorId") Long mentorId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.mentorId = :mentorId GROUP BY r.rating")
    List<Object[]> getRatingDistribution(@Param("mentorId") Long mentorId);
    long countByMentorId(Long mentorId);
}
