package com.mjsec.lms.repository;

import com.mjsec.lms.domain.PlanComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanCommentRepository extends JpaRepository<PlanComment, Long> {

    public List<PlanComment> findAllByAssignmentAssignId(Long assignId);
}
