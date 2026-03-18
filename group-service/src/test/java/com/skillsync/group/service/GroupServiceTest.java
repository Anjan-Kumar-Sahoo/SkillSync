package com.skillsync.group.service;

import com.skillsync.group.dto.CreateGroupRequest;
import com.skillsync.group.dto.GroupResponse;
import com.skillsync.group.entity.GroupMember;
import com.skillsync.group.entity.LearningGroup;
import com.skillsync.group.repository.DiscussionRepository;
import com.skillsync.group.repository.GroupMemberRepository;
import com.skillsync.group.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private DiscussionRepository discussionRepository;

    @InjectMocks private GroupService groupService;

    private LearningGroup testGroup;

    @BeforeEach
    void setUp() {
        testGroup = LearningGroup.builder()
                .id(1L)
                .name("Java Learners")
                .description("Learn Java together")
                .maxMembers(10)
                .createdBy(100L)
                .members(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Create group - success")
    void createGroup_shouldCreateAndReturn() {
        CreateGroupRequest request = new CreateGroupRequest("Java Learners", "Learn Java together", 10);
        when(groupRepository.save(any(LearningGroup.class))).thenReturn(testGroup);
        when(memberRepository.save(any(GroupMember.class))).thenReturn(null);

        GroupResponse response = groupService.createGroup(100L, request);

        assertNotNull(response);
        assertEquals("Java Learners", response.name());
        verify(groupRepository).save(any(LearningGroup.class));
        verify(memberRepository).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("Join group - already a member throws exception")
    void joinGroup_shouldThrowForExistingMember() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 100L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> groupService.joinGroup(1L, 100L));
    }

    @Test
    @DisplayName("Join group - full group throws exception")
    void joinGroup_shouldThrowWhenGroupFull() {
        testGroup.setMaxMembers(1);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(false);
        when(memberRepository.countByGroupId(1L)).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> groupService.joinGroup(1L, 200L));
    }

    @Test
    @DisplayName("Get group by ID - success")
    void getGroupById_shouldReturnGroup() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        GroupResponse response = groupService.getGroupById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Get group by ID - not found throws exception")
    void getGroupById_shouldThrowWhenNotFound() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> groupService.getGroupById(999L));
    }

    @Test
    @DisplayName("Leave group - owner cannot leave")
    void leaveGroup_shouldThrowForOwner() {
        GroupMember owner = GroupMember.builder().group(testGroup).userId(100L).role(GroupMember.MemberRole.OWNER).build();
        when(memberRepository.findByGroupIdAndUserId(1L, 100L)).thenReturn(Optional.of(owner));

        assertThrows(RuntimeException.class, () -> groupService.leaveGroup(1L, 100L));
    }
}
