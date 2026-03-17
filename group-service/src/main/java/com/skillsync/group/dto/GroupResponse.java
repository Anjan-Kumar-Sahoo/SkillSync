package com.skillsync.group.dto;

import java.time.LocalDateTime;

public record GroupResponse(Long id, String name, String description, int maxMembers, int memberCount, Long createdBy, LocalDateTime createdAt) {}
