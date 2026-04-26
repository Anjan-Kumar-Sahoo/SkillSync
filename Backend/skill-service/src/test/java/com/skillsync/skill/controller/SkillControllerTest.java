package com.skillsync.skill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skill.dto.CreateSkillRequest;
import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.service.command.SkillCommandService;
import com.skillsync.skill.service.query.SkillQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
class SkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private SkillCommandService skillCommandService;
    @MockitoBean private SkillQueryService skillQueryService;

    @Test
    @DisplayName("GET /api/skills - returns paged results")
    void getAllSkills_shouldReturnPage() throws Exception {
        when(skillQueryService.getAllSkills(any())).thenReturn(new PageImpl<>(List.of(
                new SkillResponse(1L, "Java", "Programming", "Java lang", true)
        ), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/skills").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Java"));
    }

    @Test
    @DisplayName("GET /api/skills/{id} - returns skill")
    void getSkillById_shouldReturn200() throws Exception {
        SkillResponse response = new SkillResponse(1L, "Java", "Programming", "Java lang", true);
        when(skillQueryService.getSkillById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    @DisplayName("GET /api/skills/batch - returns results for ids")
    void getSkillsByIds_shouldReturn200() throws Exception {
        when(skillQueryService.getSkillsByIds(List.of(1L, 2L))).thenReturn(List.of(
                new SkillResponse(1L, "Java", "Programming", "Java lang", true),
                new SkillResponse(2L, "Spring", "Programming", "Spring framework", true)
        ));

        mockMvc.perform(get("/api/skills/batch").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].name").value("Spring"));
    }

    @Test
    @DisplayName("POST /api/skills - creates skill")
    void createSkill_shouldReturn201() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest("Java", "Programming", "Java lang");
        SkillResponse response = new SkillResponse(1L, "Java", "Programming", "Java lang", true);
        when(skillCommandService.createSkill(any())).thenReturn(response);

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    @DisplayName("POST /api/skills - invalid payload returns 400")
    void createSkill_shouldReturn400ForInvalidPayload() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest("", "Programming", "Java lang");

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/skills/{id} - updates skill")
    void updateSkill_shouldReturn200() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest("Java 21", "Programming", "Java lang");
        SkillResponse response = new SkillResponse(1L, "Java 21", "Programming", "Java lang", true);
        when(skillCommandService.updateSkill(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/skills/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java 21"));
    }

    @Test
    @DisplayName("GET /api/skills/search - returns results")
    void searchSkills_shouldReturn200() throws Exception {
        when(skillQueryService.searchSkills("Java")).thenReturn(List.of(
                new SkillResponse(1L, "Java", "Programming", "Java lang", true)));

        mockMvc.perform(get("/api/skills/search").param("q", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} - deactivates skill")
    void deactivateSkill_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/skills/1"))
                .andExpect(status().isOk());
    }
}
