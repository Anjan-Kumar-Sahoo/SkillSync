package com.skillsync.skill.service.command;

import com.skillsync.cache.CacheService;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillCommandServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private CacheService cacheService;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private SkillCommandService service;

    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testSkill = Skill.builder().id(1L).name("Java").category("Programming")
                .description("Java lang").isActive(true).build();
    }

    @Test @DisplayName("createSkill - success with cache invalidation and event")
    void createSkill_success() {
        when(skillRepository.existsByName("Python")).thenReturn(false);
        Skill saved = Skill.builder().id(2L).name("Python").category("Programming")
                .description("Python lang").isActive(true).build();
        when(skillRepository.save(any())).thenReturn(saved);

        SkillResponse resp = service.createSkill(new CreateSkillRequest("Python", "Programming", "Python lang"));
        assertEquals("Python", resp.name());
        verify(cacheService).evictByPattern(CacheService.vKey("skill:all:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:search:*"));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("skill.created"), (Object) any());
    }

    @Test @DisplayName("createSkill - duplicate throws")
    void createSkill_duplicate() {
        when(skillRepository.existsByName("Java")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.createSkill(new CreateSkillRequest("Java", "x", "x")));
    }

    @Test @DisplayName("updateSkill - success")
    void updateSkill_success() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any())).thenReturn(testSkill);

        SkillResponse resp = service.updateSkill(1L, new CreateSkillRequest("Java Updated", "Lang", "Updated"));
        assertNotNull(resp);
        verify(cacheService).evict(CacheService.vKey("skill:1"));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("skill.updated"), (Object) any());
    }

    @Test @DisplayName("updateSkill - not found")
    void updateSkill_notFound() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateSkill(1L, new CreateSkillRequest("x", "x", "x")));
    }

    @Test @DisplayName("deactivateSkill - success")
    void deactivateSkill_success() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));

        service.deactivateSkill(1L);
        assertFalse(testSkill.isActive());
        verify(skillRepository).save(testSkill);
        verify(cacheService).evict(CacheService.vKey("skill:1"));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("skill.updated"), (Object) any());
    }

    @Test @DisplayName("deactivateSkill - not found")
    void deactivateSkill_notFound() {
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deactivateSkill(1L));
    }

    @Test @DisplayName("publishEvent swallows exceptions")
    void publishEvent_swallows() {
        when(skillRepository.existsByName("New")).thenReturn(false);
        Skill saved = Skill.builder().id(3L).name("New").category("C").description("D").isActive(true).build();
        when(skillRepository.save(any())).thenReturn(saved);
        doThrow(new RuntimeException("down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

        assertDoesNotThrow(() -> service.createSkill(new CreateSkillRequest("New", "C", "D")));
    }
}
