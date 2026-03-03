package com.mjsec.lms.attendance.dto;

import com.mjsec.lms.attendance.domain.type.AttendanceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeeklyAttendanceResponse {

    private Long studentNumber;

    private String name;

    private AttendanceType attendanceType;

    private String week;
}