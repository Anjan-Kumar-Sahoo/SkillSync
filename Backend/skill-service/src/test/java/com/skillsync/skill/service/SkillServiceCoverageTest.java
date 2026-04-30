package com.skillsync.skill.service;

import com.skillsync.skill.dto.CreateSkillRequest;
import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceCoverageTest {
    @Test
    @DisplayName("createSkill throws NPE for null request")
    void createSkill_shouldThrowForNullRequest() {
        assertThrows(NullPointerException.class, () -> skillService.createSkill(null));
    }

    @Test
    @DisplayName("updateSkill throws NPE for null request")
    void updateSkill_shouldThrowForNullRequest() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(activeSkill));
        assertThrows(NullPointerException.class, () -> skillService.updateSkill(1L, null));
    }

    @Test
    @DisplayName("getSkillById throws RuntimeException for null id")
    void getSkillById_shouldThrowForNullId() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> skillService.getSkillById(null));
        assertEquals("Skill not found: null", ex.getMessage());
    }

    @Test
    @DisplayName("deactivateSkill throws RuntimeException for null id")
    void deactivateSkill_shouldThrowForNullId() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> skillService.deactivateSkill(null));
        assertEquals("Skill not found: null", ex.getMessage());
    }

    @Test
    @DisplayName("searchSkills with null query returns empty list")
    void searchSkills_shouldReturnEmptyForNullQuery() {
        when(skillRepository.searchByName(null)).thenReturn(List.of());
        List<SkillResponse> result = skillService.searchSkills(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("searchSkills with empty query returns empty list")
    void searchSkills_shouldReturnEmptyForEmptyQuery() {
        when(skillRepository.searchByName("")).thenReturn(List.of());
        List<SkillResponse> result = skillService.searchSkills("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllSkills with null pageable throws NPE")
    void getAllSkills_shouldThrowForNullPageable() {
        assertThrows(NullPointerException.class, () -> skillService.getAllSkills(null));
    }

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    private Skill activeSkill;

    @BeforeEach
    void setUp() {
        activeSkill = Skill.builder()
                .id(1L)
                .name("Java")
                .category("Programming")
                .description("Java fundamentals")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getAllSkills maps repository results")
    void getAllSkills_shouldMapResults() {
        Pageable pageable = PageRequest.of(0, 10);
        when(skillRepository.findByIsActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(activeSkill)));

        Page<SkillResponse> result = skillService.getAllSkills(pageable);

        assertEquals(1, result.getTotalElements());
        SkillResponse mapped = result.getContent().get(0);
        assertEquals("Java", mapped.name());
        assertEquals("Programming", mapped.category());
        verify(skillRepository).findByIsActiveTrue(pageable);
    }

    @Test
    @DisplayName("getSkillById returns mapped skill when present")
    void getSkillById_shouldReturnMappedSkill() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(activeSkill));

        SkillResponse response = skillService.getSkillById(1L);

        assertEquals(1L, response.id());
        assertEquals("Java", response.name());
    }

    @Test
    @DisplayName("getSkillById throws when skill is missing")
    void getSkillById_shouldThrowWhenMissing() {
        when(skillRepository.findById(55L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> skillService.getSkillById(55L));

        assertEquals("Skill not found: 55", ex.getMessage());
    }

    @Test
    @DisplayName("searchSkills maps custom query results")
    void searchSkills_shouldMapResults() {
        when(skillRepository.searchByName("ja")).thenReturn(List.of(activeSkill));

        List<SkillResponse> result = skillService.searchSkills("ja");

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).name());
        verify(skillRepository).searchByName("ja");
    }

    @Test
    @DisplayName("createSkill throws when name already exists")
    void createSkill_shouldThrowForDuplicateName() {
        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java fundamentals");
        when(skillRepository.existsByName("Java")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> skillService.createSkill(request));

        assertEquals("Skill already exists: Java", ex.getMessage());
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("createSkill persists new active skill")
    void createSkill_shouldPersistSkill() {
        CreateSkillRequest request = new CreateSkillRequest("Python", "Programming", "Python basics");
        Skill saved = Skill.builder()
                .id(2L)
                .name("Python")
                .category("Programming")
                .description("Python basics")
                .isActive(true)
                .build();
        when(skillRepository.existsByName("Python")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenReturn(saved);

        SkillResponse response = skillService.createSkill(request);

        assertEquals(2L, response.id());
        assertEquals("Python", response.name());

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(captor.capture());
        Skill toPersist = captor.getValue();
        assertTrue(toPersist.isActive());
        assertEquals("Python", toPersist.getName());
        assertEquals("Programming", toPersist.getCategory());
        assertEquals("Python basics", toPersist.getDescription());
    }

    @Test
    @DisplayName("updateSkill throws when target skill is missing")
    void updateSkill_shouldThrowWhenMissing() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> skillService.updateSkill(99L, new CreateSkillRequest("Go", "Programming", "Go lang")));

        assertEquals("Skill not found: 99", ex.getMessage());
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("updateSkill mutates existing skill and saves")
    void updateSkill_shouldMutateAndSave() {
        Skill existing = Skill.builder()
                .id(1L)
                .name("Java")
                .category("Programming")
                .description("Old")
                .isActive(true)
                .build();
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(existing)).thenReturn(existing);

        SkillResponse response = skillService.updateSkill(1L, new CreateSkillRequest("Java 21", "Backend", "Updated"));

        assertEquals("Java 21", response.name());
        assertEquals("Backend", response.category());
        assertEquals("Updated", response.description());
        verify(skillRepository).save(existing);
    }

    @Test
    @DisplayName("deactivateSkill throws when target skill is missing")
    void deactivateSkill_shouldThrowWhenMissing() {
        when(skillRepository.findById(100L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> skillService.deactivateSkill(100L));

        assertEquals("Skill not found: 100", ex.getMessage());
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("deactivateSkill marks skill inactive and saves")
    void deactivateSkill_shouldMarkInactive() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(activeSkill));

        skillService.deactivateSkill(1L);

        assertFalse(activeSkill.isActive());
        verify(skillRepository).save(activeSkill);
    }
}
