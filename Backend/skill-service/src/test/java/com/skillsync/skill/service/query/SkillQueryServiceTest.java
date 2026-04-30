package com.skillsync.skill.service.query;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillQueryServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private CacheService cacheService;
    @InjectMocks private SkillQueryService service;

    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testSkill = Skill.builder().id(1L).name("Java").category("Programming")
                .description("Java lang").isActive(true).build();
    }

    @Test @DisplayName("getSkillById - cache miss loads from DB")
    void getSkillById_cacheMiss() {
        when(cacheService.getOrLoad(anyString(), eq(SkillResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SkillResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        SkillResponse resp = service.getSkillById(1L);
        assertNotNull(resp);
        assertEquals("Java", resp.name());
    }

    @Test @DisplayName("getSkillById - not found returns null")
    void getSkillById_notFound() {
        when(cacheService.getOrLoad(anyString(), eq(SkillResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SkillResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(skillRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(service.getSkillById(1L));
    }

    @Test @DisplayName("getAllSkills")
    void getAllSkills() {
        Page<Skill> page = new PageImpl<>(List.of(testSkill));
        when(skillRepository.findByIsActiveTrue(PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getAllSkills(PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("searchSkills")
    void searchSkills() {
        when(skillRepository.searchByName("Java")).thenReturn(List.of(testSkill));
        assertEquals(1, service.searchSkills("Java").size());
    }

    @Test @DisplayName("getSkillsByIds - with ids")
    void getSkillsByIds() {
        when(skillRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(testSkill));
        assertEquals(1, service.getSkillsByIds(List.of(1L, 2L)).size());
    }

    @Test @DisplayName("getSkillsByIds - null ids returns empty")
    void getSkillsByIds_null() {
        assertEquals(0, service.getSkillsByIds(null).size());
    }

    @Test @DisplayName("getSkillsByIds - empty ids returns empty")
    void getSkillsByIds_empty() {
        assertEquals(0, service.getSkillsByIds(List.of()).size());
    }
}
