package com.mjsec.lms.repository;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.type.GroupMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    @Query("SELECT gm.role FROM GroupMember gm WHERE gm.user.userId = :userId AND gm.studyGroup.studyId = :studyId")
    Optional<GroupMemberRole> findRoleByUserIdAndStudyId(@Param("userId") Long userId, @Param("studyId") Long studyId);

    Optional<GroupMember> findByUserAndStudyGroup(User user, StudyGroup studyGroup);

    // User가 속한 모든 스터디 그룹 조회
    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.studyGroup WHERE gm.user.userId = :userId")
    List<GroupMember> findByUserIdWithStudyGroup(@Param("userId") Long userId);

    //특정 스터디 그룹의 멘티들 조회
    List<GroupMember> findByStudyGroup_StudyIdAndRole(Long studyId, GroupMemberRole role);

    //특정 스터디 그룹의 모든 멤버 조회
    List<GroupMember> findByStudyGroup_StudyId(Long studyId);

    // 유저와 스터디 그룹 정보를 받아 이미 있는 멤버인지 확인
    boolean existsByUserAndStudyGroup(User user, StudyGroup studyGroup);

    @Modifying
    @Query("DELETE FROM GroupMember g WHERE g.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM GroupMember g WHERE g.studyGroup.creator = :user")
    void deleteByStudyGroupCreator(@Param("user") User user);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.studyGroup WHERE gm.user = :user AND gm.role = :role")
    List<GroupMember> findByUserAndRoleWithStudyGroup(@Param("user") User user, @Param("role") GroupMemberRole role);
}
