package com.mjsec.lms.repository;

import com.mjsec.lms.domain.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    boolean existsBySubmitterUserIdAndAssignmentAssignId(Long submitterUserId, Long assignmentAssignId);

    Optional<AssignmentSubmission> findBySubmitterUserIdAndAssignmentAssignId(Long submitterUserId, Long assignmentAssignId);

    List<AssignmentSubmission> findAssignmentSubmissionsByAssignmentAssignId(Long assignmentAssignId);
}
