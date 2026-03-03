package com.mjsec.lms.attendance.dto;

import com.mjsec.lms.attendance.domain.type.AttendanceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponse {

    private Long attendanceId;

    private Long userId;

    private AttendanceType type;

    private LocalDate attendanceDate;

    private LocalDateTime createdAt;

}
