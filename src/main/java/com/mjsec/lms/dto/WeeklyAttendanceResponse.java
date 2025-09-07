package com.mjsec.lms.dto;

import com.mjsec.lms.type.AttendanceType;
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