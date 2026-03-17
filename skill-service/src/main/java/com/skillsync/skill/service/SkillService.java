package com.skillsync.skill.service;

import com.skillsync.skill.dto.*;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class SkillService {
    private final SkillRepository skillRepository;

    public Page<SkillResponse> getAllSkills(Pageable pageable) {
        return skillRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
    }

    public SkillResponse getSkillById(Long id) {
        return skillRepository.findById(id).map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
    }

    public List<SkillResponse> searchSkills(String query) {
        return skillRepository.searchByName(query).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SkillResponse createSkill(CreateSkillRequest request) {
        if (skillRepository.existsByName(request.name())) {
            throw new RuntimeException("Skill already exists: " + request.name());
        }
        Skill skill = Skill.builder().name(request.name()).category(request.category())
                .description(request.description()).isActive(true).build();
        return mapToResponse(skillRepository.save(skill));
    }

    public SkillResponse updateSkill(Long id, CreateSkillRequest request) {
        Skill skill = skillRepository.findById(id).orElseThrow(() -> new RuntimeException("Skill not found: " + id));
        skill.setName(request.name());
        skill.setCategory(request.category());
        skill.setDescription(request.description());
        return mapToResponse(skillRepository.save(skill));
    }

    public void deactivateSkill(Long id) {
        Skill skill = skillRepository.findById(id).orElseThrow(() -> new RuntimeException("Skill not found: " + id));
        skill.setActive(false);
        skillRepository.save(skill);
    }

    private SkillResponse mapToResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory(), skill.getDescription(), skill.isActive());
    }
}
