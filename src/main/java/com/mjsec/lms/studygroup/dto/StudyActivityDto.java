package com.mjsec.lms.studygroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import com.mjsec.lms.attendance.dto.StudyAttendanceDto;

@Data
@Builder
public class StudyActivityDto {

    @NotBlank(message = "제목을 입력해주세요!")
    private String title;

    private String content;

    private List<StudyAttendanceDto> studyAttendanceDtoList;

    @Pattern(regexp = "^\\d{1,2}주차$", message = "주차는 'N주차' 형식이어야 합니다")
    private String week;

    private List<String> imageUrls;
}
