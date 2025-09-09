package com.mjsec.lms.dto.study;

import com.mjsec.lms.type.AttendanceType;
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
