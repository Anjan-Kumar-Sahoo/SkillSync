package com.skillsync.skill.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.skill.config.RabbitMQConfig;
import com.skillsync.skill.dto.CreateSkillRequest;
import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
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

    @InjectMocks private SkillCommandService skillCommandService;

    @Test
    @DisplayName("createSkill — persists, invalidates cache, and publishes event")
    void createSkill_shouldPersistInvalidateAndPublish() {
        when(skillRepository.existsByName("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });

        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java programming language");
        SkillResponse result = skillCommandService.createSkill(request);

        assertNotNull(result);
        assertEquals("Java", result.name());
        assertEquals("Programming", result.category());
        verify(skillRepository).save(any(Skill.class));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:all:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:search:*"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.SKILL_EXCHANGE), eq("skill.created"), any(com.skillsync.skill.event.SkillEvent.class));
    }

    @Test
    @DisplayName("createSkill — duplicate name throws and skips side effects")
    void createSkill_shouldRejectDuplicateSkillName() {
        when(skillRepository.existsByName("Java")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> skillCommandService.createSkill(new CreateSkillRequest("Java", "Programming", "desc")));

        assertTrue(ex.getMessage().contains("Skill already exists"));
        verify(skillRepository, never()).save(any(Skill.class));
        verify(cacheService, never()).evictByPattern(anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(com.skillsync.skill.event.SkillEvent.class));
    }

    @Test
    @DisplayName("updateSkill — updates fields, invalidates cache, and publishes event")
    void updateSkill_shouldUpdateInvalidateAndPublish() {
        Skill existing = Skill.builder()
                .id(10L)
                .name("Old")
                .category("Legacy")
                .description("old")
                .isActive(true)
                .build();
        when(skillRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SkillResponse result = skillCommandService.updateSkill(10L,
                new CreateSkillRequest("New", "Programming", "updated"));

        assertEquals("New", result.name());
        assertEquals("Programming", result.category());
        verify(cacheService).evict(CacheService.vKey("skill:10"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:all:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:search:*"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.SKILL_EXCHANGE), eq("skill.updated"), any(com.skillsync.skill.event.SkillEvent.class));
    }

    @Test
    @DisplayName("updateSkill — missing entity throws")
    void updateSkill_shouldThrowWhenMissing() {
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> skillCommandService.updateSkill(999L, new CreateSkillRequest("X", "Y", "Z")));
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("deactivateSkill — marks inactive, invalidates cache, and publishes event")
    void deactivateSkill_shouldMarkInactiveInvalidateAndPublish() {
        Skill existing = Skill.builder()
                .id(11L)
                .name("Java")
                .category("Programming")
                .description("desc")
                .isActive(true)
                .build();
        when(skillRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        skillCommandService.deactivateSkill(11L);

        assertFalse(existing.isActive());
        verify(skillRepository).save(existing);
        verify(cacheService).evict(CacheService.vKey("skill:11"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:all:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:search:*"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.SKILL_EXCHANGE), eq("skill.updated"), any(com.skillsync.skill.event.SkillEvent.class));
    }

    @Test
    @DisplayName("deactivateSkill — missing entity throws")
    void deactivateSkill_shouldThrowWhenMissing() {
        when(skillRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> skillCommandService.deactivateSkill(404L));
        verify(skillRepository, never()).save(any(Skill.class));
    }
}
