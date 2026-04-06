package com.skillsync.user.service;

import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.entity.UserSkill;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final ProfileRepository profileRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillServiceClient skillServiceClient;

    public ProfileResponse getProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for userId: " + userId));
        List<SkillSummary> skills = getSkillsForUser(userId);
        return mapToResponse(profile, skills);
    }

    public ProfileResponse getProfileById(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
        List<SkillSummary> skills = getSkillsForUser(profile.getUserId());
        return mapToResponse(profile, skills);
    }

    @Transactional
    public ProfileResponse createOrUpdateProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElse(Profile.builder().userId(userId).build());

        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.location() != null) profile.setLocation(request.location());

        profile.setProfileCompletePct(calculateCompleteness(profile));
        profile = profileRepository.save(profile);

        List<SkillSummary> skills = getSkillsForUser(userId);
        log.info("Profile updated for userId: {}", userId);
        return mapToResponse(profile, skills);
    }

    @Transactional
    public void addSkill(Long userId, AddSkillRequest request) {
        if (userSkillRepository.existsByUserIdAndSkillId(userId, request.skillId())) {
            throw new RuntimeException("Skill already added to profile");
        }
        UserSkill userSkill = UserSkill.builder()
                .userId(userId)
                .skillId(request.skillId())
                .proficiency(UserSkill.Proficiency.valueOf(request.proficiency()))
                .build();
        userSkillRepository.save(userSkill);
        log.info("Skill {} added for userId: {}", request.skillId(), userId);
    }

    @Transactional
    public void removeSkill(Long userId, Long skillId) {
        userSkillRepository.deleteByUserIdAndSkillId(userId, skillId);
        log.info("Skill {} removed for userId: {}", skillId, userId);
    }

    public Page<Profile> getAllProfiles(Pageable pageable) {
        return profileRepository.findAll(pageable);
    }

    private List<SkillSummary> getSkillsForUser(Long userId) {
        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);
        List<SkillSummary> skills = new ArrayList<>();
        for (UserSkill us : userSkills) {
            try {
                skills.add(skillServiceClient.getSkillById(us.getSkillId()));
            } catch (Exception e) {
                log.warn("Could not fetch skill {}: {}", us.getSkillId(), e.getMessage());
                skills.add(new SkillSummary(us.getSkillId(), "Unknown", null));
            }
        }
        return skills;
    }

    private int calculateCompleteness(Profile profile) {
        int score = 0;
        if (profile.getFirstName() != null) score += 20;
        if (profile.getLastName() != null) score += 20;
        if (profile.getBio() != null) score += 20;
        if (profile.getPhone() != null) score += 20;
        if (profile.getLocation() != null) score += 20;
        return score;
    }

    private ProfileResponse mapToResponse(Profile profile, List<SkillSummary> skills) {
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
