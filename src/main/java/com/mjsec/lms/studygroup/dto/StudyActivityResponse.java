package com.mjsec.lms.studygroup.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.mjsec.lms.attendance.dto.StudyAttendanceDto;

//스터디 활동 글
@Data
@Builder
public class StudyActivityResponse {

    private Long activityId;

    private String title;

    private String content;

    private List<StudyAttendanceDto> studyAttendanceDtoList;

    private String week;

    private List<String> imageUrls;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
