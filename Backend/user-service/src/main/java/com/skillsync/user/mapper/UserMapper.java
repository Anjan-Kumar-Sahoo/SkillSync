package com.skillsync.user.mapper;

import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure mapping functions for User/Profile entities.
 * Used by both UserCommandService and UserQueryService (CQRS decoupling).
 * No service dependencies — only entity → DTO conversion.
 */
public final class UserMapper {

    private UserMapper() {} // Utility class

    public static ProfileResponse toProfileResponse(Profile profile, List<SkillSummary> skills) {
        return new ProfileResponse(
                profile.getId(), profile.getUserId(),
                profile.getFirstName(), profile.getLastName(),
                null, profile.getBio(), profile.getAvatarUrl(),
                profile.getPhone(), profile.getLocation(),
                profile.getProfileCompletePct(), skills,
                profile.getCreatedAt()
        );
    }
}
