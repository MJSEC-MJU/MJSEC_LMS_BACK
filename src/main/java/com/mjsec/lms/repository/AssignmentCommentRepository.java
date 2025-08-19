package com.mjsec.lms.repository;

import com.mjsec.lms.domain.AssignmentComment;
import com.mjsec.lms.dto.AssignmentCommentDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentCommentRepository extends JpaRepository<AssignmentComment, Long> {

    public List<AssignmentComment> findAllByAssignmentAssignId(Long assignId);
}
