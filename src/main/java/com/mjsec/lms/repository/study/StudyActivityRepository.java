package com.mjsec.lms.repository.study;

import com.mjsec.lms.domain.study.StudyActivity;
import com.mjsec.lms.domain.study.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
