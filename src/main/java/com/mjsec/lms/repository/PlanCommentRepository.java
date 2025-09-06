package com.mjsec.lms.repository;

import com.mjsec.lms.domain.PlanComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanCommentRepository extends JpaRepository<PlanComment, Long> {

    public List<PlanComment> findAllByPlanPlanId(Long planId);

    // 댓글 ID와 작성자 ID로 댓글 조회
    Optional<PlanComment> findByCommentIdAndAuthor_UserId(Long commentId, Long userId);
}
