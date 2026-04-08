package com.mjsec.lms.assignment.repository;

import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findAllByStudyGroup_StudyId(Long studyId);

    //과제가 있는 계획만 조회하기
    List<Plan> findAllByStudyGroup_StudyIdAndHasAssignmentTrue(Long groupId);

    // 마감된 과제 조회하기
    @Query("SELECT a FROM Plan a WHERE a.endDate < :currentTime")
    List<Plan> findExpiredPlansAndHasAssignmentTrue(@Param("currentTime") LocalDateTime currentTime);

    //특정 시간 범위 내에서 마감된 과제 조회하기
    @Query("SELECT a FROM Plan a WHERE a.endDate BETWEEN :startTime AND :endTime")
    List<Plan> findPlansExpiredBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    //특정 스터디 그룹의 마감된 과제들 조회
    @Query("SELECT p FROM Plan p WHERE p.studyGroup.studyId = :studyId AND p.endDate < :currentTime AND p.hasAssignment = true")
    List<Plan> findExpiredAssignmentsByStudyGroup(
            @Param("studyId") Long studyId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("SELECT p FROM Plan p WHERE p.endDate BETWEEN :startTime AND :endTime AND p.hasAssignment = true")
    List<Plan> findAssignmentsExpiredBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Plan p WHERE p.planId = :planId")
    Optional<Plan>  findByIdWithPessimisticLock(@Param("planId") Long planId);

    @Modifying
    @Query("DELETE FROM Plan p WHERE p.creator = :user")
    void deleteByCreator(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Plan p WHERE p.studyGroup.creator = :user")
    void deleteByStudyGroupCreator(@Param("user") User user);
}
