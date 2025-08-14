package com.mjsec.lms.repository;

import com.mjsec.lms.domain.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    boolean existsBySubmitterUserIdAndAssignmentAssignId(Long submitterUserId, Long assignmentAssignId);
}
