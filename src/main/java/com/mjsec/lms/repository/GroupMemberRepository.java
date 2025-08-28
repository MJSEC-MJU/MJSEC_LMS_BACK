package com.mjsec.lms.repository;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.type.GroupMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
