package com.skillsync.skill.service;

import com.skillsync.skill.dto.*;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @InjectMocks private SkillService service;

    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testSkill = Skill.builder().id(1L).name("Java").category("Programming")
                .description("Java language").isActive(true).build();
    }

    @Test @DisplayName("getAllSkills")
    void getAllSkills() {
        Page<Skill> page = new PageImpl<>(List.of(testSkill));
        when(skillRepository.findByIsActiveTrue(PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getAllSkills(PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getSkillById - found")
    void getSkillById() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        SkillResponse resp = service.getSkillById(1L);
        assertEquals("Java", resp.name());
    }

    @Test @DisplayName("getSkillById - not found")
    void getSkillById_notFound() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getSkillById(1L));
    }

    @Test @DisplayName("searchSkills")
    void searchSkills() {
        when(skillRepository.searchByName("Java")).thenReturn(List.of(testSkill));
        assertEquals(1, service.searchSkills("Java").size());
    }

    @Test @DisplayName("createSkill - success")
    void createSkill() {
        when(skillRepository.existsByName("Python")).thenReturn(false);
        Skill saved = Skill.builder().id(2L).name("Python").category("Programming")
                .description("Python lang").isActive(true).build();
        when(skillRepository.save(any())).thenReturn(saved);
        SkillResponse resp = service.createSkill(new CreateSkillRequest("Python", "Programming", "Python lang"));
        assertEquals("Python", resp.name());
    }

    @Test @DisplayName("createSkill - duplicate throws")
    void createSkill_duplicate() {
        when(skillRepository.existsByName("Java")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.createSkill(new CreateSkillRequest("Java", "Programming", "x")));
    }

    @Test @DisplayName("updateSkill - success")
    void updateSkill() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any())).thenReturn(testSkill);
        SkillResponse resp = service.updateSkill(1L, new CreateSkillRequest("Java Updated", "Lang", "Updated"));
        assertNotNull(resp);
    }

    @Test @DisplayName("updateSkill - not found")
    void updateSkill_notFound() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateSkill(1L, new CreateSkillRequest("x", "x", "x")));
    }

    @Test @DisplayName("deactivateSkill - success")
    void deactivateSkill() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        service.deactivateSkill(1L);
        assertFalse(testSkill.isActive());
        verify(skillRepository).save(testSkill);
    }

    @Test @DisplayName("deactivateSkill - not found")
    void deactivateSkill_notFound() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deactivateSkill(1L));
    }
}
