package com.skillsync.user.dto;

import java.time.LocalTime;

public record AvailabilitySlotResponse(Long id, int dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isActive) {}
