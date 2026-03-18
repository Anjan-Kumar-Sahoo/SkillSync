package com.skillsync.skill.service;

import com.skillsync.skill.dto.CreateSkillRequest;
import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @InjectMocks private SkillService skillService;

    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testSkill = Skill.builder()
                .id(1L)
                .name("Java")
                .category("Programming")
                .description("Java programming language")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Get skill by ID - success")
    void getSkillById_shouldReturnSkill() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));

        SkillResponse response = skillService.getSkillById(1L);

        assertEquals("Java", response.name());
        assertEquals("Programming", response.category());
    }

    @Test
    @DisplayName("Get skill by ID - not found throws exception")
    void getSkillById_shouldThrowWhenNotFound() {
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> skillService.getSkillById(999L));
    }

    @Test
    @DisplayName("Create skill - success")
    void createSkill_shouldSaveAndReturn() {
        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java lang");
        when(skillRepository.existsByName("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        SkillResponse response = skillService.createSkill(request);

        assertNotNull(response);
        assertEquals("Java", response.name());
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    @DisplayName("Create skill - duplicate throws exception")
    void createSkill_shouldThrowForDuplicate() {
        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java lang");
        when(skillRepository.existsByName("Java")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> skillService.createSkill(request));
        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Search skills - returns results")
    void searchSkills_shouldReturnMatchingSkills() {
        when(skillRepository.searchByName("Java")).thenReturn(List.of(testSkill));

        List<SkillResponse> results = skillService.searchSkills("Java");

        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).name());
    }

    @Test
    @DisplayName("Deactivate skill - success")
    void deactivateSkill_shouldSetInactive() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any())).thenReturn(testSkill);

        skillService.deactivateSkill(1L);

        assertFalse(testSkill.isActive());
        verify(skillRepository).save(testSkill);
    }
}
