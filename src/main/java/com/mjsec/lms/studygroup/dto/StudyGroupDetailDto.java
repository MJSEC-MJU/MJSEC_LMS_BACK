package com.mjsec.lms.studygroup.dto;

import com.mjsec.lms.studygroup.domain.type.StudyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudyGroupDetailDto {

    private Long studyId;

    private String name;

    private String content;

    private String category;

    private String generation;

    private String studyImage;

    private StudyStatus status;

    // 멘토 정보
    private String mentorName;

    private Long mentorStudentNumber;

    // 멘티 리스트
    private List<MenteeInfo> menteeList;

    private int menteeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class MenteeInfo {
        private String name;
        private Long studentNumber;
        private String email;
    }
}