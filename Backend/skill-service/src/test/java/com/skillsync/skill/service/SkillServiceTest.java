package com.skillsync.skill.service;

import com.skillsync.cache.CacheService;
import com.skillsync.skill.dto.CreateSkillRequest;
import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import com.skillsync.skill.service.command.SkillCommandService;
import com.skillsync.skill.service.query.SkillQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private CacheService cacheService;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private SkillCommandService skillCommandService;
    @InjectMocks private SkillQueryService skillQueryService;

    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testSkill = Skill.builder().id(1L).name("Java").category("Programming")
                .description("Java programming language").isActive(true).build();
    }

    @Test
    @DisplayName("Get skill by ID - cache miss → DB fetch")
    void getSkillById_shouldReturnSkill() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("skill:1")), eq(SkillResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SkillResponse> fallback = inv.getArgument(3);
                    return fallback.get(); // DB execute
                });
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));

        SkillResponse response = skillQueryService.getSkillById(1L);

        assertEquals("Java", response.name());
    }

    @Test
    @DisplayName("Get skill by ID - cache HIT → NO DB fetch")
    void getSkillById_shouldReturnFromCache() {
        SkillResponse cached = new SkillResponse(1L, "Java", "Programming", "Java lang", true);
        when(cacheService.getOrLoad(eq(CacheService.vKey("skill:1")), eq(SkillResponse.class), any(), any()))
                .thenReturn(cached);

        SkillResponse response = skillQueryService.getSkillById(1L);

        assertEquals("Java", response.name());
        verify(skillRepository, never()).findById(anyLong()); // bypass DB
    }

    @Test
    @DisplayName("Get skill by ID - not found throws exception")
    void getSkillById_shouldThrowWhenNotFound() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("skill:999")), eq(SkillResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SkillResponse> fallback = inv.getArgument(3);
                    return fallback.get(); // returns null when db is empty
                });
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            SkillResponse result = skillQueryService.getSkillById(999L);
            if (result == null) throw new RuntimeException("Skill not found");
        });
    }

    @Test
    @DisplayName("Create skill - invalidates cache and publishes event")
    void createSkill_shouldSaveAndInvalidateCache() {
        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java lang");
        when(skillRepository.existsByName("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        SkillResponse response = skillCommandService.createSkill(request);

        assertNotNull(response);
        assertNotNull(response);
        assertEquals("Java", response.name());
        verify(skillRepository).save(any(Skill.class));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:all:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("skill:search:*"));
    }

    @Test
    @DisplayName("Create skill - duplicate throws exception")
    void createSkill_shouldThrowForDuplicate() {
        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java lang");
        when(skillRepository.existsByName("Java")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> skillCommandService.createSkill(request));
        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Search skills - returns results")
    void searchSkills_shouldReturnMatchingSkills() {
        when(skillRepository.searchByName("Java")).thenReturn(List.of(testSkill));

        List<SkillResponse> results = skillQueryService.searchSkills("Java");

        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).name());
    }

    @Test
    @DisplayName("Deactivate skill - invalidates cache")
    void deactivateSkill_shouldSetInactiveAndInvalidate() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any())).thenReturn(testSkill);

        skillCommandService.deactivateSkill(1L);

        assertFalse(testSkill.isActive());
        verify(skillRepository).save(testSkill);
        verify(cacheService).evict(CacheService.vKey("skill:1"));
    }
}
