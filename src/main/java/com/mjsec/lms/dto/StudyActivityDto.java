package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudyActivityDto {

    private String title;

    private String content;

    private List<StudyAttendanceDto> studyAttendanceDtoList;

    private String week;
}
