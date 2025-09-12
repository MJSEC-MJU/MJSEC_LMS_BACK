package com.mjsec.lms.repository;

import com.mjsec.lms.domain.Attendance;
import com.mjsec.lms.domain.StudyActivity;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    //중복 체크
    boolean existsByUserAndStudyGroupAndAttendanceDate(
            User user, StudyGroup studyGroup, LocalDate attendanceDate);

    //출결 조회용
    List<Attendance> findByStudyGroupAndAttendanceDateBetween(
            StudyGroup studyGroup, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByUserAndStudyGroupOrderByAttendanceDateDesc(
            User user, StudyGroup studyGroup);

    List<Attendance> findByUserAndStudyGroupAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            User user, StudyGroup studyGroup, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByUserAndStudyGroupAndAttendanceDateGreaterThanEqualOrderByAttendanceDateAsc(
            User user, StudyGroup studyGroup, LocalDate startDate);

    List<Attendance> findByUserAndStudyGroupAndAttendanceDateLessThanEqualOrderByAttendanceDateAsc(
            User user, StudyGroup studyGroup, LocalDate endDate);

    // 특정 스터디 그룹의 특정 주차 출석 조회
    List<Attendance> findByStudyGroupAndWeekOrderByUserStudentNumberAsc(StudyGroup studyGroup, String week);

    // 특정 스터디 그룹의 모든 출석 조회 (주차별 정렬용)
    List<Attendance> findByStudyGroupOrderByWeekAscUserStudentNumberAsc(StudyGroup studyGroup);

    List<Attendance> findByStudyActivity(StudyActivity studyActivity);

    // 특정 날짜의 모든 출석 기록 조회
    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.studyActivity.creator = :user")
    void deleteByStudyActivityCreator(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.studyGroup.creator = :user")
    void deleteByStudyGroupCreator(@Param("user") User user);

    @Query("SELECT a.attendanceId FROM Attendance a " +
            "WHERE a.user.userId = :userId " +
            "AND a.studyGroup.studyId = :studyId")
    List<Long> findIdsByUserIdAndStudyGroupId(@Param("userId") Long userId, @Param("studyId") Long studyId);
}
