package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.mapper.GroupMapper;
import com.skillsync.user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupCommandServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private DiscussionRepository discussionRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private CacheService cacheService;

    @InjectMocks private GroupCommandService groupCommandService;

    private LearningGroup testGroup;

    @BeforeEach
    void setUp() {
        testGroup = LearningGroup.builder()
                .id(1L).name("Java Learners").description("Study group")
                .category("Programming").maxMembers(50).createdBy(100L)
                .members(new ArrayList<>()).build();
    }

    // ─── CREATE GROUP ───

    @Nested
    @DisplayName("createGroup")
    class CreateGroup {
        @Test
        @DisplayName("admin can create group")
        void shouldCreateGroupForAdmin() {
            CreateGroupRequest request = new CreateGroupRequest("New Group", "desc", "Tech", 30);
            when(groupRepository.save(any(LearningGroup.class))).thenReturn(testGroup);
            when(memberRepository.save(any(GroupMember.class))).thenReturn(mock(GroupMember.class));

            GroupResponse result = groupCommandService.createGroup(100L, "ROLE_ADMIN", request);

            assertNotNull(result);
            verify(groupRepository).save(any(LearningGroup.class));
            verify(memberRepository).save(any(GroupMember.class));
            verify(cacheService).evictByPattern(contains("user:group:all:"));
        }

        @Test
        @DisplayName("non-admin cannot create group")
        void shouldThrowForNonAdmin() {
            CreateGroupRequest request = new CreateGroupRequest("New Group", "desc", "Tech", 30);

            assertThrows(RuntimeException.class,
                    () -> groupCommandService.createGroup(100L, "ROLE_LEARNER", request));
        }

        @Test
        @DisplayName("null maxMembers defaults to Integer.MAX_VALUE")
        void shouldDefaultMaxMembers() {
            CreateGroupRequest request = new CreateGroupRequest("Group", "desc", null, null);
            when(groupRepository.save(any(LearningGroup.class))).thenReturn(testGroup);
            when(memberRepository.save(any(GroupMember.class))).thenReturn(mock(GroupMember.class));

            groupCommandService.createGroup(100L, "ROLE_ADMIN", request);

            verify(groupRepository).save(argThat(g -> g.getMaxMembers() == Integer.MAX_VALUE));
        }
    }

    // ─── JOIN GROUP ───

    @Nested
    @DisplayName("joinGroup")
    class JoinGroup {
        @Test
        @DisplayName("user can join group")
        void shouldJoinGroup() {
            when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
            when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(false);

            groupCommandService.joinGroup(1L, 200L);

            verify(memberRepository).save(any(GroupMember.class));
            verify(cacheService).evict(contains("user:group:1"));
        }

        @Test
        @DisplayName("already a member throws exception")
        void shouldThrowForDuplicateMember() {
            when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
            when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(true);

            assertThrows(RuntimeException.class, () -> groupCommandService.joinGroup(1L, 200L));
        }

        @Test
        @DisplayName("non-existent group throws exception")
        void shouldThrowForNonExistentGroup() {
            when(groupRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> groupCommandService.joinGroup(999L, 200L));
        }
    }

    // ─── LEAVE GROUP ───

    @Nested
    @DisplayName("leaveGroup")
    class LeaveGroup {
        @Test
        @DisplayName("member can leave group")
        void shouldLeaveGroup() {
            GroupMember member = GroupMember.builder()
                    .id(1L).group(testGroup).userId(200L).role(GroupMember.MemberRole.MEMBER).build();
            when(memberRepository.findByGroupIdAndUserId(1L, 200L)).thenReturn(Optional.of(member));

            groupCommandService.leaveGroup(1L, 200L);

            verify(memberRepository).delete(member);
        }

        @Test
        @DisplayName("owner cannot leave group")
        void shouldThrowForOwner() {
            GroupMember owner = GroupMember.builder()
                    .id(1L).group(testGroup).userId(100L).role(GroupMember.MemberRole.OWNER).build();
            when(memberRepository.findByGroupIdAndUserId(1L, 100L)).thenReturn(Optional.of(owner));

            assertThrows(RuntimeException.class, () -> groupCommandService.leaveGroup(1L, 100L));
        }

        @Test
        @DisplayName("non-member cannot leave")
        void shouldThrowForNonMember() {
            when(memberRepository.findByGroupIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> groupCommandService.leaveGroup(1L, 999L));
        }
    }

    // ─── UPDATE GROUP ───

    @Test
    @DisplayName("updateGroup — admin can update group fields")
    void updateGroup_shouldUpdateFields() {
        UpdateGroupRequest request = new UpdateGroupRequest("Updated", "new desc", "Science", 40);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(LearningGroup.class))).thenReturn(testGroup);
        when(memberRepository.countByGroupId(1L)).thenReturn(5L);
        when(memberRepository.existsByGroupIdAndUserId(1L, 100L)).thenReturn(true);

        GroupResponse result = groupCommandService.updateGroup(1L, 100L, "ROLE_ADMIN", request);

        assertNotNull(result);
        verify(groupRepository).save(any(LearningGroup.class));
    }

    @Test
    @DisplayName("updateGroup — non-admin cannot update")
    void updateGroup_shouldThrowForNonAdmin() {
        UpdateGroupRequest request = new UpdateGroupRequest("Updated", null, null, null);

        assertThrows(RuntimeException.class,
                () -> groupCommandService.updateGroup(1L, 100L, "ROLE_LEARNER", request));
    }

    // ─── DELETE GROUP ───

    @Test
    @DisplayName("deleteGroup — admin can delete group")
    void deleteGroup_shouldDelete() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        groupCommandService.deleteGroup(1L, 100L, "ROLE_ADMIN");

        verify(groupRepository).delete(testGroup);
    }

    // ─── ADD MEMBER ───

    @Test
    @DisplayName("addMember — admin can add member by email")
    void addMember_shouldAdd() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Map<String, Object> usersPage = Map.of("content", List.of(
                Map.of("id", 200, "email", "john@test.com", "firstName", "John", "lastName", "Doe")));
        when(authServiceClient.getAllUsers(0, 50, null, "john@test.com")).thenReturn(usersPage);
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(false);
        when(memberRepository.save(any(GroupMember.class))).thenReturn(
                GroupMember.builder().id(1L).group(testGroup).userId(200L).role(GroupMember.MemberRole.MEMBER).build());

        AddGroupMemberRequest request = new AddGroupMemberRequest("john@test.com");
        GroupMemberResponse result = groupCommandService.addMember(1L, 100L, "ROLE_ADMIN", request);

        assertNotNull(result);
        verify(memberRepository).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("addMember — throws for already-existing member")
    void addMember_shouldThrowForExistingMember() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Map<String, Object> usersPage = Map.of("content", List.of(
                Map.of("id", 200, "email", "john@test.com")));
        when(authServiceClient.getAllUsers(0, 50, null, "john@test.com")).thenReturn(usersPage);
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> groupCommandService.addMember(1L, 100L, "ROLE_ADMIN", new AddGroupMemberRequest("john@test.com")));
    }

    // ─── REMOVE MEMBER ───

    @Test
    @DisplayName("removeMember — admin can remove member")
    void removeMember_shouldRemove() {
        GroupMember member = GroupMember.builder()
                .id(1L).group(testGroup).userId(200L).role(GroupMember.MemberRole.MEMBER).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 200L)).thenReturn(Optional.of(member));

        groupCommandService.removeMember(1L, 200L, 100L, "ROLE_ADMIN");

        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("removeMember — cannot remove owner")
    void removeMember_shouldThrowForOwner() {
        GroupMember owner = GroupMember.builder()
                .id(1L).group(testGroup).userId(100L).role(GroupMember.MemberRole.OWNER).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 100L)).thenReturn(Optional.of(owner));

        assertThrows(RuntimeException.class,
                () -> groupCommandService.removeMember(1L, 100L, 200L, "ROLE_ADMIN"));
    }

    // ─── POST DISCUSSION ───

    @Test
    @DisplayName("postDiscussion — member can post")
    void postDiscussion_shouldPost() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(true);
        Discussion saved = Discussion.builder().id(1L).group(testGroup).authorId(200L)
                .title("Title").content("Content").build();
        when(discussionRepository.save(any(Discussion.class))).thenReturn(saved);
        when(authServiceClient.getUserById(200L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "role", "ROLE_LEARNER"));

        PostDiscussionRequest request = new PostDiscussionRequest("Title", "Content", null);
        DiscussionResponse result = groupCommandService.postDiscussion(1L, 200L, "ROLE_LEARNER", request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("postDiscussion — non-member cannot post")
    void postDiscussion_shouldThrowForNonMember() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> groupCommandService.postDiscussion(1L, 200L, "ROLE_LEARNER",
                        new PostDiscussionRequest("Title", "Content", null)));
    }

    // ─── DELETE DISCUSSION ───

    @Test
    @DisplayName("deleteDiscussion — admin can delete any discussion")
    void deleteDiscussion_shouldDeleteForAdmin() {
        Discussion discussion = Discussion.builder().id(5L).group(testGroup).authorId(200L).build();
        when(discussionRepository.findById(5L)).thenReturn(Optional.of(discussion));
        when(discussionRepository.countByParentId(5L)).thenReturn(0L);

        groupCommandService.deleteDiscussion(1L, 5L, 100L, "ROLE_ADMIN");

        verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("deleteDiscussion — cannot delete discussion with replies")
    void deleteDiscussion_shouldThrowIfHasReplies() {
        Discussion discussion = Discussion.builder().id(5L).group(testGroup).authorId(100L).build();
        when(discussionRepository.findById(5L)).thenReturn(Optional.of(discussion));
        when(memberRepository.existsByGroupIdAndUserId(1L, 100L)).thenReturn(true);
        when(discussionRepository.countByParentId(5L)).thenReturn(3L);

        assertThrows(RuntimeException.class,
                () -> groupCommandService.deleteDiscussion(1L, 5L, 100L, "ROLE_LEARNER"));
    }

    @Test
    @DisplayName("deleteDiscussion — wrong group throws exception")
    void deleteDiscussion_shouldThrowForWrongGroup() {
        LearningGroup otherGroup = LearningGroup.builder().id(2L).build();
        Discussion discussion = Discussion.builder().id(5L).group(otherGroup).authorId(200L).build();
        when(discussionRepository.findById(5L)).thenReturn(Optional.of(discussion));

        assertThrows(RuntimeException.class,
                () -> groupCommandService.deleteDiscussion(1L, 5L, 100L, "ROLE_ADMIN"));
    }

    @Test
    @DisplayName("deleteDiscussionById — hit the wrapper")
    void deleteDiscussionById_shouldHitWrapper() {
        Discussion discussion = Discussion.builder().id(5L).group(testGroup).authorId(100L).build();
        when(discussionRepository.findById(5L)).thenReturn(Optional.of(discussion));
        when(discussionRepository.countByParentId(5L)).thenReturn(0L);

        groupCommandService.deleteDiscussionById(5L, 100L, "ROLE_ADMIN");

        verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("postDiscussion — with parent")
    void postDiscussion_WithParent() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(true);
        Discussion parent = Discussion.builder().id(10L).group(testGroup).build();
        when(discussionRepository.findById(10L)).thenReturn(Optional.of(parent));
        
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(i -> i.getArgument(0));
        when(authServiceClient.getUserById(200L)).thenReturn(Map.of("email", "j@t.com"));

        PostDiscussionRequest request = new PostDiscussionRequest("Title", "Content", 10L);
        groupCommandService.postDiscussion(1L, 200L, "ROLE_LEARNER", request);

        verify(discussionRepository).save(argThat(d -> d.getParent() != null));
    }

    @Test
    @DisplayName("postDiscussion — parent wrong group")
    void postDiscussion_ParentWrongGroup() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(true);
        LearningGroup otherGroup = LearningGroup.builder().id(2L).build();
        Discussion parent = Discussion.builder().id(10L).group(otherGroup).build();
        when(discussionRepository.findById(10L)).thenReturn(Optional.of(parent));

        PostDiscussionRequest request = new PostDiscussionRequest("Title", "Content", 10L);
        assertThrows(RuntimeException.class, () -> groupCommandService.postDiscussion(1L, 200L, "ROLE_LEARNER", request));
    }

    @Test
    @DisplayName("canDeleteDiscussion — mentor can delete learner msg")
    void canDeleteDiscussion_MentorDeletesLearner() {
        Discussion discussion = Discussion.builder().id(5L).group(testGroup).authorId(200L).build();
        when(discussionRepository.findById(5L)).thenReturn(Optional.of(discussion));
        when(memberRepository.existsByGroupIdAndUserId(1L, 101L)).thenReturn(true);
        when(discussionRepository.countByParentId(5L)).thenReturn(0L);
        // Author is learner
        when(authServiceClient.getUserById(200L)).thenReturn(Map.of("role", "ROLE_LEARNER"));

        groupCommandService.deleteDiscussion(1L, 5L, 101L, "ROLE_MENTOR");

        verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("canDeleteDiscussion — learner cannot delete others")
    void canDeleteDiscussion_LearnerCannotDeleteOthers() {
        Discussion discussion = Discussion.builder().id(5L).group(testGroup).authorId(200L).build();
        when(discussionRepository.findById(5L)).thenReturn(Optional.of(discussion));
        when(memberRepository.existsByGroupIdAndUserId(1L, 101L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> groupCommandService.deleteDiscussion(1L, 5L, 101L, "ROLE_LEARNER"));
    }

    @Test
    @DisplayName("resolveUserByEmail — handles invalid list content")
    void resolveUserByEmail_InvalidContent() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        // auth service returns something that is not a list of maps
        when(authServiceClient.getAllUsers(anyInt(), anyInt(), any(), anyString()))
                .thenReturn(Map.of("content", List.of("string-not-map")));

        assertThrows(RuntimeException.class, () -> groupCommandService.addMember(1L, 100L, "ROLE_ADMIN", new AddGroupMemberRequest("j@t.com")));
    }

    @Test
    @DisplayName("toLong — handles parsing error")
    void toLong_ParsingError() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(authServiceClient.getAllUsers(anyInt(), anyInt(), any(), anyString()))
                .thenReturn(Map.of("content", List.of(Map.of("id", "invalid", "email", "j@t.com"))));

        assertThrows(RuntimeException.class, () -> groupCommandService.addMember(1L, 100L, "ROLE_ADMIN", new AddGroupMemberRequest("j@t.com")));
    }

    @Test
    @DisplayName("updateGroup — partial fields")
    void updateGroup_PartialFields() {
        UpdateGroupRequest request = new UpdateGroupRequest(" ", null, "  ", null);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(LearningGroup.class))).thenReturn(testGroup);
        
        groupCommandService.updateGroup(1L, 100L, "ROLE_ADMIN", request);
        
        verify(groupRepository).save(testGroup);
    }
}
