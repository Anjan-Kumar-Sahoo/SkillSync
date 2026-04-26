package com.skillsync.skill.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillQueryServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private CacheService cacheService;

    @InjectMocks private SkillQueryService skillQueryService;

    private Skill buildSkill() {
        return Skill.builder()
                .id(1L)
                .name("Java")
                .category("Programming")
                .description("Java lang")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getAllSkills — returns paginated skills")
    void shouldReturnAllSkills() {
        Pageable pageable = PageRequest.of(0, 10);
        when(skillRepository.findByIsActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(buildSkill())));

        var result = skillQueryService.getAllSkills(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Java", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("getSkillById — cache miss loads from DB")
    void shouldGetSkillById() {
        Skill skill = buildSkill();
        when(cacheService.getOrLoad(anyString(), eq(SkillResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SkillResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        SkillResponse result = skillQueryService.getSkillById(1L);

        assertNotNull(result);
        assertEquals("Java", result.name());
    }

    @Test
    @DisplayName("getSkillById — returns null for non-existent")
    void shouldReturnNullForMissing() {
        when(cacheService.getOrLoad(anyString(), eq(SkillResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SkillResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        SkillResponse result = skillQueryService.getSkillById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("searchSkills — returns matching skills")
    void shouldSearchSkills() {
        when(skillRepository.searchByName("java"))
                .thenReturn(List.of(buildSkill()));

        var result = skillQueryService.searchSkills("java");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getSkillsByIds — returns skills by IDs")
    void shouldGetSkillsByIds() {
        when(skillRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(buildSkill()));

        var result = skillQueryService.getSkillsByIds(List.of(1L, 2L));

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getSkillsByIds — empty input returns empty list without hitting repository")
    void shouldReturnEmptyForMissingIds() {
        var result = skillQueryService.getSkillsByIds(List.of());

        assertTrue(result.isEmpty());
        verify(skillRepository, never()).findAllById(anyList());
    }
}
