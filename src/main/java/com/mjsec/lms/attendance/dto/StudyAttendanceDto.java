package com.mjsec.lms.attendance.dto;

import com.mjsec.lms.attendance.domain.type.AttendanceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
//유저 출석체크용 Dto
public class StudyAttendanceDto {

    private Long studentNumber;

    private String name;

    private AttendanceType type;
}
