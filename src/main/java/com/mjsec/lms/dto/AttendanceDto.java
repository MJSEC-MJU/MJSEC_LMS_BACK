package com.mjsec.lms.dto;

import com.mjsec.lms.type.AttendanceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {

    @NotNull(message = "type은 필수입니다.")
    private AttendanceType type;

    @NotNull(message = "출석 날짜는 필수입니다.")
    private LocalDate attendanceDate;
}
