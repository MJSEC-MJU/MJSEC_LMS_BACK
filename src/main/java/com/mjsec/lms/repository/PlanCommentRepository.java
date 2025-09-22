package com.mjsec.lms.repository;

import com.mjsec.lms.domain.PlanComment;
import com.mjsec.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanCommentRepository extends JpaRepository<PlanComment, Long> {

    List<PlanComment> findAllByPlanPlanId(Long planId);

    // 댓글 ID와 작성자 ID로 댓글 조회
    Optional<PlanComment> findByCommentIdAndAuthor_UserId(Long commentId, Long userId);

    @Modifying
    @Query("DELETE FROM PlanComment p WHERE p.author = :user")
    void deleteByAuthor(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM PlanComment p WHERE p.plan.creator = :user")
    void deleteByPlanCreator(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM PlanComment p WHERE p.plan.studyGroup.creator = :user")
    void deleteByStudyGroupCreator(@Param("user") User user);

    @Query("SELECT p.commentId FROM PlanComment p " +
            "WHERE p.author.userId = :userId " +
            "AND p.plan.studyGroup.studyId = :studyId")
    List<Long> findIdsByUserIdAndStudyGroupId(@Param("userId") Long userId, @Param("studyId") Long studyId);
}
