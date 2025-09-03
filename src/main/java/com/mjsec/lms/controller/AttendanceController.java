package com.mjsec.lms.controller;

import com.mjsec.lms.dto.AttendanceDto;
import com.mjsec.lms.dto.AttendanceResponse;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.AttendanceService;
import com.mjsec.lms.type.ResponseMessage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    //출석 체크
    @PostMapping("/{groupId}/attendance/{studentNumber}")
    public ResponseEntity<SuccessResponse<AttendanceResponse>> createAttendance(
            @RequestBody AttendanceDto dto,
            @PathVariable Long groupId,
            @PathVariable Long studentNumber,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        AttendanceResponse attendanceResponse = attendanceService.createAttendance(groupId,studentNumber,dto,currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ATTENDANCE_CREATE_SUCCESS,
                        attendanceResponse
                )
        );
    }

    //유저별로 출석체크 리스트 조회
    @GetMapping("/{groupId}/attendance/user/{studentNumber}")
    public ResponseEntity<SuccessResponse<List<AttendanceResponse>>> getAttendanceByUser(
            @PathVariable Long groupId,
            @PathVariable Long studentNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<AttendanceResponse> attendanceResponseList = attendanceService.getAttendanceByDateRange(
                groupId, studentNumber, startDate, endDate, currentUserStudentNumber);

        return ResponseEntity.ok(
                        SuccessResponse.of(
                                ResponseMessage.ATTENDANCE_GET_SUCCESS,
                                attendanceResponseList
                        )
                );
    }
}
