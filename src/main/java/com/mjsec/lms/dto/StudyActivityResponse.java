package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

//스터디 활동 글
@Data
@Builder
public class StudyActivityResponse {

    private Long StudyActivityId;

    private String title;

    private String content;

    private List<StudyAttendanceDto> studyAttendanceDtoList;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
