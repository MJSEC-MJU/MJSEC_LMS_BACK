package com.mjsec.lms.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.mjsec.lms.attendance.service.AttendanceAlertService;

@Service
@Slf4j
public class ScheduledTaskService {

    private final WeeklyAlertService weeklyAlertService;
    private final AttendanceAlertService attendanceAlertService;

    ScheduledTaskService(WeeklyAlertService weeklyAlertService,
                         AttendanceAlertService attendanceAlertService) {

        this.weeklyAlertService = weeklyAlertService;
        this.attendanceAlertService = attendanceAlertService;
    }

    //매주 월요일 0시에 체크
    @Scheduled(cron = "0 0 0 * * MON")
    public void checkAssignmentNotSubmitted() {
        log.info("Scheduled task started: checkAssignmentNotSubmitted");
        try {
            weeklyAlertService.checkAndSendWeeklyAssignmentReport();
            log.info("Scheduled task completed successfully: checkAssignmentNotSubmitted");
        } catch (Exception e) {
            log.error("Scheduled task failed: checkAssignmentNotSubmitted", e);
        }
    }

    // 매일 오전 9시에 10일 경과 결석자 체크
    @Scheduled(cron = "0 0 9 * * *")
    public void checkAbsenceAlert() {
        log.info("Scheduled task started: checkAbsenceAlert");
        try {
            attendanceAlertService.checkAndSendAbsenceAlert();
            log.info("Scheduled task completed successfully: checkAbsenceAlert");
        } catch (Exception e) {
            log.error("Scheduled task failed: checkAbsenceAlert", e);
        }
    }

    /*
    //테스트용 - 30초마다 실행 (개발/테스트시에만 사용) - 출석체크 알람

    //@Scheduled(fixedRate = 30000)
    public void testAbsenceScheduler() {
        log.info("Test absence scheduler executed at: {}", java.time.LocalDateTime.now());
        // 테스트 시에는 아래 주석을 해제해서 사용
        attendanceAlertService.checkAndSendAbsenceAlert();
    }
    
    // 테스트용 - 30초마다 실행 (개발/테스트시에만 사용) - 과제 알람
    //@Scheduled(fixedRate = 30000)
    public void testScheduler() {
        log.info("Test scheduler executed at: {}", java.time.LocalDateTime.now());
        // 테스트 시에는 아래 주석을 해제해서 사용
        weeklyAlertService.checkAndSendWeeklyAssignmentReport();
    }
     */
}