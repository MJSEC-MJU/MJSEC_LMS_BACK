package com.mjsec.lms.repository;

import com.mjsec.lms.domain.Attendance;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
