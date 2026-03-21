package com.skillsync.user.group.service;

import com.skillsync.user.group.dto.*;
import com.skillsync.user.group.entity.*;
import com.skillsync.user.group.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

@Service @RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final DiscussionRepository discussionRepository;

    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        LearningGroup group = LearningGroup.builder()
                .name(request.name()).description(request.description())
                .maxMembers(request.maxMembers()).createdBy(userId)
                .members(new ArrayList<>()).build();
        group = groupRepository.save(group);

        GroupMember owner = GroupMember.builder().group(group).userId(userId)
                .role(GroupMember.MemberRole.OWNER).build();
        memberRepository.save(owner);

        return mapToResponse(group);
    }

    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        LearningGroup group = findGroup(groupId);
        if (memberRepository.existsByGroupIdAndUserId(groupId, userId)) throw new RuntimeException("Already a member");
        long count = memberRepository.countByGroupId(groupId);
        if (count >= group.getMaxMembers()) throw new RuntimeException("Group is full");
        memberRepository.save(GroupMember.builder().group(group).userId(userId).role(GroupMember.MemberRole.MEMBER).build());
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a member of this group"));
        if (member.getRole() == GroupMember.MemberRole.OWNER) throw new RuntimeException("Owner cannot leave the group");
        memberRepository.delete(member);
    }

    public Page<GroupResponse> getAllGroups(Pageable pageable) {
        return groupRepository.findAll(pageable).map(this::mapToResponse);
    }

    public GroupResponse getGroupById(Long id) { return mapToResponse(findGroup(id)); }

    @Transactional
    public DiscussionResponse postDiscussion(Long groupId, Long userId, PostDiscussionRequest request) {
        LearningGroup group = findGroup(groupId);
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) throw new RuntimeException("Must be a member to post");
        Discussion parent = request.parentId() != null ? discussionRepository.findById(request.parentId()).orElse(null) : null;
        Discussion discussion = Discussion.builder().group(group).authorId(userId).content(request.content()).parent(parent).build();
        discussion = discussionRepository.save(discussion);
        return mapDiscussion(discussion);
    }

    public Page<DiscussionResponse> getDiscussions(Long groupId, Pageable pageable) {
        return discussionRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable).map(this::mapDiscussion);
    }

    private LearningGroup findGroup(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> new RuntimeException("Group not found: " + id));
    }

    private GroupResponse mapToResponse(LearningGroup g) {
        int count = g.getMembers() != null ? g.getMembers().size() : (int) memberRepository.countByGroupId(g.getId());
        return new GroupResponse(g.getId(), g.getName(), g.getDescription(), g.getMaxMembers(), count, g.getCreatedBy(), g.getCreatedAt());
    }

    private DiscussionResponse mapDiscussion(Discussion d) {
        return new DiscussionResponse(d.getId(), d.getGroup().getId(), d.getAuthorId(), d.getContent(),
                d.getParent() != null ? d.getParent().getId() : null, d.getCreatedAt());
    }
}
