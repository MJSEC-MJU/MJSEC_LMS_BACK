package com.mjsec.lms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScheduledTaskService {

    private final WeeklyAlertService weeklyAlertService;

    public ScheduledTaskService(WeeklyAlertService weeklyAlertService) {
        this.weeklyAlertService = weeklyAlertService;
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


    /*
    테스트용!!!
     */
    //@Scheduled(fixedRate = 30000)
    public void testScheduler() {
        log.info("Test scheduler executed at: {}", java.time.LocalDateTime.now());
        // 테스트 시에는 아래 주석을 해제해서 사용
        weeklyAlertService.checkAndSendWeeklyAssignmentReport();
    }
}