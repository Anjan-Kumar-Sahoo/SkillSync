package com.skillsync.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.user.dto.*;
import com.skillsync.user.service.command.GroupCommandService;
import com.skillsync.user.service.query.GroupQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private GroupCommandService groupCommandService;
    @MockBean private GroupQueryService groupQueryService;

    private final GroupResponse sampleGroup = new GroupResponse(
            1L, "Java Learners", "Study group", "Programming", 50, 5, 100L, LocalDateTime.now(), true);

    // ─── QUERY ENDPOINTS ───

    @Test
    @DisplayName("GET /api/groups — returns paginated groups")
    void getAllGroups_shouldReturnPage() throws Exception {
        when(groupQueryService.getAllGroups(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleGroup)));

        mockMvc.perform(get("/api/groups").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Java Learners"));
    }

    @Test
    @DisplayName("GET /api/groups/my — returns user's groups")
    void getMyGroups_shouldReturnUserGroups() throws Exception {
        when(groupQueryService.getMyGroups(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleGroup)));

        mockMvc.perform(get("/api/groups/my").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Java Learners"));
    }

    @Test
    @DisplayName("GET /api/groups/{id} — returns single group")
    void getGroup_shouldReturnGroup() throws Exception {
        when(groupQueryService.getGroupById(1L, 1L)).thenReturn(sampleGroup);

        mockMvc.perform(get("/api/groups/1").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java Learners"));
    }

    @Test
    @DisplayName("GET /api/groups/{id}/members — returns members page")
    void getMembers_shouldReturnPage() throws Exception {
                GroupMemberResponse member = new GroupMemberResponse(1L, 100L, "John Doe", "john@test.com", "MEMBER", LocalDateTime.now());
        when(groupQueryService.getGroupMembers(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member)));

        mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/groups/{id}/discussions — returns discussions page")
    void getDiscussions_shouldReturnPage() throws Exception {
        when(groupQueryService.getDiscussions(eq(1L), eq(1L), eq("ROLE_LEARNER"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/groups/1/discussions")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_LEARNER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/groups/{id}/messages — alias for discussions")
    void getMessages_shouldReturnPage() throws Exception {
        when(groupQueryService.getDiscussions(eq(1L), eq(1L), eq("ROLE_ADMIN"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/groups/1/messages")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    // ─── COMMAND ENDPOINTS ───

    @Test
    @DisplayName("POST /api/groups — creates group (201)")
    void createGroup_shouldCreateAndReturn201() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest("New Group", "desc", "Tech", 30);
        when(groupCommandService.createGroup(eq(1L), eq("ROLE_ADMIN"), any(CreateGroupRequest.class)))
                .thenReturn(sampleGroup);

        mockMvc.perform(post("/api/groups")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java Learners"));
    }

    @Test
    @DisplayName("PUT /api/groups/{id} — updates group")
    void updateGroup_shouldUpdate() throws Exception {
        UpdateGroupRequest request = new UpdateGroupRequest("Updated", "new desc", "Science", 40);
        when(groupCommandService.updateGroup(eq(1L), eq(1L), eq("ROLE_ADMIN"), any(UpdateGroupRequest.class)))
                .thenReturn(sampleGroup);

        mockMvc.perform(put("/api/groups/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/groups/{id} — deletes group")
    void deleteGroup_shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/groups/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        verify(groupCommandService).deleteGroup(1L, 1L, "ROLE_ADMIN");
    }

    @Test
    @DisplayName("POST /api/groups/{id}/join — joins group")
    void joinGroup_shouldJoin() throws Exception {
        mockMvc.perform(post("/api/groups/1/join").header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(groupCommandService).joinGroup(1L, 1L);
    }

    @Test
    @DisplayName("POST /api/groups/{id}/leave — leaves group")
    void leaveGroup_shouldLeave() throws Exception {
        mockMvc.perform(post("/api/groups/1/leave").header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(groupCommandService).leaveGroup(1L, 1L);
    }

    @Test
    @DisplayName("POST /api/groups/{id}/members — adds member (201)")
    void addMember_shouldAddAndReturn201() throws Exception {
        AddGroupMemberRequest request = new AddGroupMemberRequest("john@test.com");
        GroupMemberResponse memberResponse = new GroupMemberResponse(
                1L, 100L, "John Doe", "john@test.com", "MEMBER", LocalDateTime.now());
        when(groupCommandService.addMember(eq(1L), eq(1L), eq("ROLE_ADMIN"), any(AddGroupMemberRequest.class)))
                .thenReturn(memberResponse);

        mockMvc.perform(post("/api/groups/1/members")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/groups/{id}/members/{userId} — removes member")
    void removeMember_shouldRemove() throws Exception {
        mockMvc.perform(delete("/api/groups/1/members/100")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        verify(groupCommandService).removeMember(1L, 100L, 1L, "ROLE_ADMIN");
    }

    @Test
    @DisplayName("POST /api/groups/{id}/discussions — posts discussion (201)")
    void postDiscussion_shouldPostAndReturn201() throws Exception {
        PostDiscussionRequest request = new PostDiscussionRequest("Title", "Content", null);
        DiscussionResponse disc = new DiscussionResponse(
                1L, 1L, 1L, "John Doe", "ROLE_LEARNER", "Title", "Content", null, 0, Instant.now(), true);
        when(groupCommandService.postDiscussion(eq(1L), eq(1L), eq("ROLE_LEARNER"), any(PostDiscussionRequest.class)))
                .thenReturn(disc);

        mockMvc.perform(post("/api/groups/1/discussions")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_LEARNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/groups/{id}/discussions/{discussionId} — deletes discussion")
    void deleteDiscussion_shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/groups/1/discussions/5")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        verify(groupCommandService).deleteDiscussion(1L, 5L, 1L, "ROLE_ADMIN");
    }

    @Test
    @DisplayName("DELETE /api/groups/message/{discussionId} — deletes message by ID")
    void deleteMessage_shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/groups/message/5")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        verify(groupCommandService).deleteDiscussionById(5L, 1L, "ROLE_ADMIN");
    }
}
