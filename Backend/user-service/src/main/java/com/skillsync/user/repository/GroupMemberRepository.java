package com.skillsync.user.repository;

import com.skillsync.user.entity.GroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);
    Page<GroupMember> findByGroupId(Long groupId, Pageable pageable);
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupId(Long groupId);

    @Query("SELECT gm.group.id FROM GroupMember gm WHERE gm.userId = :userId AND gm.group.id IN :groupIds")
    List<Long> findJoinedGroupIds(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);
}
