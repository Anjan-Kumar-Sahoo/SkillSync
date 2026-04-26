package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.exception.ResourceNotFoundException;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.repository.*;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupQueryServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private DiscussionRepository discussionRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private CacheService cacheService;

    @InjectMocks private GroupQueryService groupQueryService;

    private LearningGroup testGroup;

    @BeforeEach
    void setUp() {
        testGroup = LearningGroup.builder()
                .id(1L).name("Java Learners").description("Study group")
                .category("Programming").maxMembers(50).createdBy(100L)
                .members(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("getGroupById — cache miss loads from DB")
    void getGroupById_shouldLoadFromDbOnCacheMiss() {
        when(cacheService.getOrLoad(anyString(), eq(GroupResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<GroupResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.countByGroupId(1L)).thenReturn(5L);
        when(memberRepository.existsByGroupIdAndUserId(1L, 100L)).thenReturn(true);

        GroupResponse result = groupQueryService.getGroupById(1L, 100L);

        assertNotNull(result);
        assertEquals("Java Learners", result.name());
        assertTrue(result.joined());
    }

    @Test
    @DisplayName("getGroupById — throws when not found")
    void getGroupById_shouldThrowWhenNotFound() {
        when(cacheService.getOrLoad(anyString(), eq(GroupResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<GroupResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> groupQueryService.getGroupById(999L, 100L));
    }

    @Test
    @DisplayName("getGroupById — null userId means not joined")
    void getGroupById_shouldReturnNotJoinedForNullUser() {
        GroupResponse cached = new GroupResponse(1L, "Java Learners", "desc", "Programming",
                50, 5, 100L, LocalDateTime.now(), false);
        when(cacheService.getOrLoad(anyString(), eq(GroupResponse.class), any(), any()))
                .thenReturn(cached);

        GroupResponse result = groupQueryService.getGroupById(1L, null);

        assertNotNull(result);
        assertFalse(result.joined());
    }

    @Test
    @DisplayName("getAllGroups — returns paginated results with join status")
    void getAllGroups_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(groupRepository.searchGroups(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(testGroup)));
        when(memberRepository.countByGroupId(1L)).thenReturn(5L);
        when(memberRepository.findJoinedGroupIds(eq(100L), anyList())).thenReturn(List.of(1L));

        Page<GroupResponse> result = groupQueryService.getAllGroups(null, null, 100L, pageable);

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).joined());
    }

    @Test
    @DisplayName("getAllGroups — null userId returns empty join set")
    void getAllGroups_shouldReturnEmptyJoinSetForNullUser() {
        Pageable pageable = PageRequest.of(0, 10);
        when(groupRepository.searchGroups(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(testGroup)));
        when(memberRepository.countByGroupId(1L)).thenReturn(5L);

        Page<GroupResponse> result = groupQueryService.getAllGroups(null, null, null, pageable);

        assertFalse(result.getContent().get(0).joined());
    }

    @Test
    @DisplayName("getMyGroups — all results are joined")
    void getMyGroups_shouldSetJoinedTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        when(groupRepository.findMyGroups(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(testGroup)));
        when(memberRepository.countByGroupId(1L)).thenReturn(5L);

        Page<GroupResponse> result = groupQueryService.getMyGroups(100L, pageable);

        assertTrue(result.getContent().get(0).joined());
    }

    @Test
    @DisplayName("getGroupMembers — returns enriched member data")
    void getGroupMembers_shouldReturnEnrichedMembers() {
        GroupMember member = GroupMember.builder()
                .id(1L).group(testGroup).userId(200L).role(GroupMember.MemberRole.MEMBER).build();
        Pageable pageable = PageRequest.of(0, 10);
        when(memberRepository.findByGroupId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(member)));
        when(authServiceClient.getUserById(200L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "email", "john@test.com"));

        Page<GroupMemberResponse> result = groupQueryService.getGroupMembers(1L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("John Doe", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("getDiscussions — non-member cannot view")
    void getDiscussions_shouldThrowForNonMember() {
        when(memberRepository.existsByGroupIdAndUserId(1L, 200L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> groupQueryService.getDiscussions(1L, 200L, "ROLE_LEARNER", PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("getDiscussions — admin can view without membership")
    void getDiscussions_shouldAllowAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        Discussion discussion = Discussion.builder().id(1L).group(testGroup).authorId(200L)
                .title("Title").content("Content").createdAt(Instant.now()).build();
        when(discussionRepository.findByGroupIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(discussion)));
        when(authServiceClient.getUserById(200L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "role", "ROLE_LEARNER"));
        when(discussionRepository.countByParentId(1L)).thenReturn(0L);

        Page<DiscussionResponse> result = groupQueryService.getDiscussions(1L, 100L, "ROLE_ADMIN", pageable);

        assertEquals(1, result.getTotalElements());
    }
}
