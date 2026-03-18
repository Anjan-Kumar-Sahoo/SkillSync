package com.skillsync.group.controller;

import com.skillsync.group.dto.GroupResponse;
import com.skillsync.group.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private GroupService groupService;

    @Test
    @DisplayName("GET /api/groups/{id} - returns group")
    void getGroup_shouldReturn200() throws Exception {
        GroupResponse response = new GroupResponse(1L, "Java Learners", "Learn Java", 10, 3, 100L, LocalDateTime.now());
        when(groupService.getGroupById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java Learners"));
    }

    @Test
    @DisplayName("POST /api/groups/{id}/join - joins group")
    void joinGroup_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/groups/1/join").header("X-User-Id", "200"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/groups/{id}/leave - leaves group")
    void leaveGroup_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/groups/1/leave").header("X-User-Id", "200"))
                .andExpect(status().isOk());
    }
}
