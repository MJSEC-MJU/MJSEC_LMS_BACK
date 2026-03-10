package com.mjsec.lms.attendance.service;

import com.mjsec.lms.attendance.domain.Attendance;
import com.mjsec.lms.attendance.repository.AttendanceRepository;
import com.mjsec.lms.attendance.domain.type.AttendanceType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.mjsec.lms.notification.external.EmailService;

@Service
@Slf4j
public class AttendanceAlertService {

    private final AttendanceRepository attendanceRepository;
    private final EmailService emailService;

    public AttendanceAlertService(AttendanceRepository attendanceRepository, EmailService emailService) {
        this.attendanceRepository = attendanceRepository;
        this.emailService = emailService;
    }

    /**
     * 10일 전 결석자에 대한 알림 발송
     * 매일 실행되어 정확히 10일 전 날짜의 결석자를 확인하고 알림 발송
     */
    @Transactional(readOnly = true)
    public void checkAndSendAbsenceAlert() {
        log.info("Starting 10-day absence alert check...");

        // 정확히 10일 전 날짜 계산
        LocalDate targetDate = LocalDate.now().minusDays(10);

        log.info("Checking absences for date: {}", targetDate);

        // 해당 날짜의 모든 출석 기록 조회
        List<Attendance> attendanceList = attendanceRepository.findByAttendanceDate(targetDate);

        if (attendanceList.isEmpty()) {
            log.info("No attendance records found for date: {}", targetDate);
            return;
        }

        // 결석(ABSENCE) 상태인 출석 기록만 필터링
        List<Attendance> absenceList = attendanceList.stream()
                .filter(attendance -> attendance.getType() == AttendanceType.ABSENCE)
                .collect(Collectors.toList());

        if (absenceList.isEmpty()) {
            log.info("No absence records found for date: {}", targetDate);
            return;
        }

        log.info("Found {} absence records for date: {}", absenceList.size(), targetDate);

        // 스터디 그룹별로 결석자 그룹핑
        Map<String, List<AbsenceInfo>> absencesByGroup = absenceList.stream()
                .map(this::createAbsenceInfo)
                .collect(Collectors.groupingBy(AbsenceInfo::getStudyGroupName));

        // 각 스터디 그룹별로 이메일 발송
        for (Map.Entry<String, List<AbsenceInfo>> entry : absencesByGroup.entrySet()) {
            String studyGroupName = entry.getKey();
            List<AbsenceInfo> absences = entry.getValue();

            try {
                emailService.sendAbsenceAlert(studyGroupName, absences, targetDate);
                log.info("Absence alert sent for study group: {} with {} absent students",
                        studyGroupName, absences.size());
            } catch (Exception e) {
                log.error("Failed to send absence alert for study group: {}", studyGroupName, e);
            }
        }

        log.info("10-day absence alert check completed successfully");
    }

    /**
     * Attendance 객체를 AbsenceInfo로 변환
     */
    private AbsenceInfo createAbsenceInfo(Attendance attendance) {
        return AbsenceInfo.builder()
                .studentNumber(attendance.getUser().getStudentNumber())
                .studentName(attendance.getUser().getName())
                .studyGroupName(attendance.getStudyGroup().getName())
                .week(attendance.getWeek())
                .absenceDate(attendance.getAttendanceDate())
                .build();
    }

    /**
     * 결석 정보를 담는 내부 클래스
     */
    @Data
    @Builder
    public static class AbsenceInfo {
        private Long studentNumber;
        private String studentName;
        private String studyGroupName;
        private String week;
        private LocalDate absenceDate;
    }
}