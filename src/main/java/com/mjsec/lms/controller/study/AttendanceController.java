package com.mjsec.lms.controller.study;

import com.mjsec.lms.dto.mentor.AttendanceDto;
import com.mjsec.lms.dto.mentor.AttendanceResponse;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.dto.study.WeeklyAttendanceResponse;
import com.mjsec.lms.service.study.AttendanceService;
import com.mjsec.lms.type.ResponseMessage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    //특정 주차의 출석체크 상태 조회
    @GetMapping("/{groupId}/attendance/week/{week}")
    public ResponseEntity<SuccessResponse<List<WeeklyAttendanceResponse>>> getAttendanceByWeek(
            @PathVariable Long groupId,
            @PathVariable String week,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<WeeklyAttendanceResponse> weeklyAttendanceList = attendanceService.getAttendanceByWeek(
                groupId, week, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.WEEKLY_ATTENDANCE_GET_SUCCESS,
                        weeklyAttendanceList
                )
        );
    }

    // 전체 주차별 출석체크 상태 조회
    @GetMapping("/{groupId}/attendance/all-weeks")
    public ResponseEntity<SuccessResponse<Map<String, List<WeeklyAttendanceResponse>>>> getAllWeeksAttendance(
            @PathVariable Long groupId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        Map<String, List<WeeklyAttendanceResponse>> allWeeksAttendance = attendanceService.getAllWeeksAttendance(
                groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ALL_WEEKS_ATTENDANCE_GET_SUCCESS,
                        allWeeksAttendance
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
