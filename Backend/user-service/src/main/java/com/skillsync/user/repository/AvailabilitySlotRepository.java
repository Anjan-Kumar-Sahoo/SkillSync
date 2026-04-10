package com.skillsync.user.repository;

import com.skillsync.user.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByMentorId(Long mentorId);

    boolean existsByMentor_IdAndDayOfWeekAndStartTimeAndEndTime(
            Long mentorId,
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );
}
