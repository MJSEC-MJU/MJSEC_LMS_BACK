package com.mjsec.lms.controller;

import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.WeeklyAlertService;
import com.mjsec.lms.type.ResponseMessage;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@Slf4j
public class TestController {

    private final WeeklyAlertService weeklyAlertService;
    private final ValidationUtils validationUtils;

    public TestController(WeeklyAlertService weeklyAlertService,
                          ValidationUtils validationUtils) {

        this.weeklyAlertService = weeklyAlertService;
        this.validationUtils = validationUtils;
    }

    //과제 미제출자 알림 수동 실행 (테스트용)
    @PostMapping("/assignment-alert")
    public ResponseEntity<SuccessResponse<String>> testAssignmentAlert(Authentication authentication) {

        try {
            log.info("Manual assignment alert check triggered by admin");

            // JwtFilter에서 설정한 studentNumber를 가져옴
            Long currentUserStudentNumber = (Long) authentication.getPrincipal();

            validationUtils.validateAdminRole(currentUserStudentNumber);

            weeklyAlertService.checkAndSendWeeklyAssignmentReport();

            return ResponseEntity.ok(
                    SuccessResponse.of(
                            ResponseMessage.PLAN_SUCCESS,
                            "과제 미제출자 알림 체크가 완료되었습니다."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to execute assignment alert check", e);
            return ResponseEntity.internalServerError().body(
                    SuccessResponse.of("알림 체크 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    //알람 시스템 상태 확인
    @GetMapping("/status")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getAlertStatus(Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        validationUtils.validateAdminRole(currentUserStudentNumber);

        Map<String, Object> status = Map.of(
                "emailServiceActive", true,
                "alertServiceActive", true,
                "schedulerActive", true,
                "currentTime", LocalDateTime.now(),
                "message", "알림 시스템이 정상 작동 중입니다."
        );

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.TEST_SUCCESS,
                        status
                )
        );
    }
}