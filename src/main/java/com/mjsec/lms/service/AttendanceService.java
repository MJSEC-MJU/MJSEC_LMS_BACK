package com.mjsec.lms.service;

import com.mjsec.lms.domain.Attendance;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AttendanceDto;
import com.mjsec.lms.dto.AttendanceResponse;
import com.mjsec.lms.repository.AttendanceRepository;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ValidationUtils validationUtils;

    public AttendanceService(AttendanceRepository attendanceRepository, ValidationUtils validationUtils) {
        this.attendanceRepository = attendanceRepository;
        this.validationUtils = validationUtils;
    }

    // 출석 체크
    @Transactional
    public AttendanceResponse createAttendance(Long groupId, Long studentNumber, AttendanceDto dto, Long currentUserStudentNumber) {

        log.info("createAttendance called");

        // 출석체크 처리하려는 사람이 멘토인지 확인
        validationUtils.validateMentoAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        // 출석체크 처리되는 사람이 스터디 그룹에 속하는지 확인
        User attendanceUser = validationUtils.validateBasicAccess(groupId, studentNumber);

        // 중복 여부 확인 (당일에 이미 출석이 있는 경우 출석처리)
        validationUtils.validateDuplicateAttendance(attendanceUser, studyGroup, dto.getAttendanceDate());

        // 출석체크 처리
        Attendance attendance = createAttendanceData(dto, attendanceUser, studyGroup);

        return createAttendanceResponse(attendance);
    }

    //유저의 출석 체크 리스트 조회하기
    public List<AttendanceResponse> getAttendanceByDateRange(Long groupId, Long studentNumber, LocalDate startDate, LocalDate endDate, Long currentUserStudentNumber) {

        log.info("getAttendanceByDateRange called");

        validationUtils.validateDateRange(startDate,endDate);
        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        User attendanceUser = validationUtils.validateMenteeAccess(groupId, studentNumber);

        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        List<Attendance> attendanceList;

        if (startDate != null && endDate != null) {
            // 둘 다 있으면 시작일 ~ 종료일 사이
            attendanceList = attendanceRepository.findByUserAndStudyGroupAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                    attendanceUser, studyGroup, startDate, endDate);
        } else if (startDate != null) {
            // 시작일만 있으면 시작일 이후 전체
            attendanceList = attendanceRepository.findByUserAndStudyGroupAndAttendanceDateGreaterThanEqualOrderByAttendanceDateAsc(
                    attendanceUser, studyGroup, startDate);
        } else if (endDate != null) {
            // 종료일만 있으면 종료일 이전 전체
            attendanceList = attendanceRepository.findByUserAndStudyGroupAndAttendanceDateLessThanEqualOrderByAttendanceDateAsc(
                    attendanceUser, studyGroup, endDate);
        } else {
            // 둘 다 null이면 전체 조회
            attendanceList = attendanceRepository.findByUserAndStudyGroupOrderByAttendanceDateDesc(attendanceUser, studyGroup);
        }

        return attendanceList.stream()
                .map(this::createAttendanceResponse)
                .toList();
    }
    
    /*
    데이터 저장용 메서드들
     */

    // 출석체크 데이터 저장하기
    private Attendance createAttendanceData(AttendanceDto dto, User user, StudyGroup studyGroup) {

        Attendance attendance = Attendance.builder()
                .user(user)
                .studyGroup(studyGroup)
                .type(dto.getType())
                .attendanceDate(dto.getAttendanceDate())
                .createdAt(LocalDateTime.now())
                .build();

        attendanceRepository.save(attendance);

        return attendance;
    }

    // 출석체크용 반환 Dto 만들기
    private AttendanceResponse createAttendanceResponse(Attendance attendance) {

        return AttendanceResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .userId(attendance.getUser().getUserId())
                .type(attendance.getType())
                .attendanceDate(attendance.getAttendanceDate())
                .createdAt(attendance.getCreatedAt())
                .build();
    }
}