package com.mjsec.lms.repository;

import com.mjsec.lms.domain.StudyActivity;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {

    @Query("SELECT sa FROM StudyActivity sa WHERE sa.studyGroup.studyId = :groupId")
    List<StudyActivity> findAllByStudyGroupId(@Param("groupId") Long groupId);

    // 특정 스터디 그룹에서 특정 주차가 이미 존재하는지 확인
    boolean existsByStudyGroupAndWeek(StudyGroup studyGroup, String week);

    // 특정 스터디 그룹에서 특정 주차를 가지면서 특정 활동 ID가 아닌 활동이 존재하는지 확인 (수정시 사용)
    boolean existsByStudyGroupAndWeekAndActivityIdNot(StudyGroup studyGroup, String week, Long activityId);

    @Modifying
    @Query("DELETE FROM StudyActivity s WHERE s.creator = :user")
    void deleteByCreator(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM StudyActivity s WHERE s.studyGroup.creator = :user")
    void deleteByStudyGroupCreator(@Param("user") User user);
}
