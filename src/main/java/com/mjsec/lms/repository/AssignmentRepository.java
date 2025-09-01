package com.mjsec.lms.repository;

import com.mjsec.lms.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findAllByStudyGroup_StudyId(Long studyId);

    // 마감된 과제 조회하기
    @Query("SELECT a FROM Assignment a WHERE a.endDate < :currentTime")
    List<Assignment> findExpiredAssignments(@Param("currentTime") LocalDateTime currentTime);

    //특정 시간 범위 내에서 마감된 과제 조회하기
    @Query("SELECT a FROM Assignment a WHERE a.endDate BETWEEN :startTime AND :endTime")
    List<Assignment> findAssignmentsExpiredBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    //특정 스터디 그룹의 마감된 과제들 조회
    @Query("SELECT a FROM Assignment a WHERE a.studyGroup.studyId = :studyId AND a.endDate < :currentTime")
    List<Assignment> findExpiredAssignmentsByStudyGroup(
            @Param("studyId") Long studyId,
            @Param("currentTime") LocalDateTime currentTime
    );
}
