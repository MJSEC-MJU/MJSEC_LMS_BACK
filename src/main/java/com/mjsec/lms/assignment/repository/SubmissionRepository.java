package com.mjsec.lms.assignment.repository;

import com.mjsec.lms.assignment.domain.AssignmentSubmission;
import com.mjsec.lms.user.domain.User;
import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    boolean existsBySubmitterUserIdAndPlanPlanId(Long submitterUserId, Long planId);

    Optional<AssignmentSubmission> findBySubmitterUserIdAndPlanPlanId(Long submitterUserId, Long planId);

    List<AssignmentSubmission> findAssignmentSubmissionsByPlanPlanId(Long planId);

    @Modifying
    @Query("DELETE FROM AssignmentSubmission a WHERE a.submitter = :user")
    void deleteBySubmitter(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM AssignmentSubmission a WHERE a.plan.creator = :user")
    void deleteByPlanCreator(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM AssignmentSubmission a WHERE a.plan.studyGroup.creator = :user")
    void deleteByStudyGroupCreator(@Param("user") User user);

    @Query("SELECT a.submissionId FROM AssignmentSubmission a " +
            "WHERE a.submitter.userId = :userId " +
            "AND a.plan.studyGroup.studyId = :studyId")
    List<Long> findIdsByUserIdAndStudyGroupId(@Param("userId") Long userId, @Param("studyId") Long studyId);

    // 특정 과제의 특정 상태 제출물들 조회
    List<AssignmentSubmission> findByPlanPlanIdAndStatus(Long planId, SubmissionStatus status);

    // 특정 사용자의 특정 과제의 특정 상태 제출물들 조회
    List<AssignmentSubmission> findByPlanPlanIdAndSubmitterUserIdAndStatus(Long planId, Long submitterUserId, SubmissionStatus status);

    // 특정 과제에 제출한 고유 제출자 수 조회 (미제출자 계산용)
    @Query("SELECT COUNT(DISTINCT a.submitter.userId) FROM AssignmentSubmission a " +
            "WHERE a.plan.planId = :planId " +
            "AND a.submitter.userId IN (" +
            "SELECT gm.user.userId FROM GroupMember gm " +
            "WHERE gm.studyGroup.studyId = :groupId AND gm.role = com.mjsec.lms.studygroup.domain.type.GroupMemberRole.MENTEE" +
            ")")
    int countDistinctMenteeSubmittersByPlanIdAndGroupId(@Param("planId") Long planId, @Param("groupId") Long groupId);


    @Query("SELECT COUNT(DISTINCT a.submitter.userId) FROM AssignmentSubmission a " +
            "WHERE a.plan.planId = :planId AND a.status = :status")
    int countDistinctSubmittersByPlanIdAndStatus(@Param("planId") Long planId, @Param("status") SubmissionStatus status);
}
