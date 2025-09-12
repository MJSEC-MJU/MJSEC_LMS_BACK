package com.mjsec.lms.repository;

import com.mjsec.lms.domain.AssignmentSubmission;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
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
}
