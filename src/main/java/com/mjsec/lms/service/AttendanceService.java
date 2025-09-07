package com.mjsec.lms.service;

import com.mjsec.lms.domain.Attendance;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AttendanceDto;
import com.mjsec.lms.dto.AttendanceResponse;
import com.mjsec.lms.dto.WeeklyAttendanceResponse;
import com.mjsec.lms.repository.AttendanceRepository;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
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

    @Transactional(readOnly = true)
    public List<WeeklyAttendanceResponse> getAttendanceByWeek(Long groupId, String week, Long currentUserStudentNumber) {

        log.info("getAttendanceByWeek called for week: {}", week);

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        List<Attendance> attendanceList = attendanceRepository.findByStudyGroupAndWeekOrderByUserStudentNumberAsc(studyGroup, week);

        return attendanceList.stream()
                .map(this::createWeeklyAttendanceResponse)
                .toList();
    }

    // 전체 주차별 출석체크 상태 조회
    @Transactional(readOnly = true)
    public Map<String, List<WeeklyAttendanceResponse>> getAllWeeksAttendance(Long groupId, Long currentUserStudentNumber) {

        log.info("getAllWeeksAttendance called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        List<Attendance> allAttendanceList = attendanceRepository.findByStudyGroupOrderByWeekAscUserStudentNumberAsc(studyGroup);

        // 주차별로 그룹핑
        Map<String, List<WeeklyAttendanceResponse>> weeklyAttendanceMap = allAttendanceList.stream()
                .map(this::createWeeklyAttendanceResponse)
                .collect(Collectors.groupingBy(WeeklyAttendanceResponse::getWeek));

        // 주차 순서대로 정렬 (1주차, 2주차, 3주차... 순서)
        return weeklyAttendanceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(this::compareWeeks))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    /*
    그 외 Private 메서드
     */

    private int compareWeeks(String week1, String week2) {
        try {
            // "N주차" 형태에서 숫자 부분만 추출
            int num1 = Integer.parseInt(week1.replaceAll("[^0-9]", ""));
            int num2 = Integer.parseInt(week2.replaceAll("[^0-9]", ""));
            return Integer.compare(num1, num2);
        } catch (NumberFormatException e) {
            // 숫자 추출 실패시 문자열 비교
            return week1.compareTo(week2);
        }
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

    // 주차별 출석체크 응답 DTO 생성
    private WeeklyAttendanceResponse createWeeklyAttendanceResponse(Attendance attendance) {
        return WeeklyAttendanceResponse.builder()
                .studentNumber(attendance.getUser().getStudentNumber())
                .name(attendance.getUser().getName())
                .attendanceType(attendance.getType())
                .week(attendance.getWeek())
                .build();
    }
}