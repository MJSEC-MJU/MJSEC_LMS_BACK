package com.mjsec.lms.dto;

import com.mjsec.lms.type.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class StudyGroupDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudyGroupRequestDto {

        @NotBlank(message = "그룹명은 필수 입력값 입니다.")
        private String name;

        @NotBlank(message = "스터디 소개는 필수 입력값 입니다.")
        private String content;

        @NotNull(message = "스터디 타입은 필수 입력값 입니다.")
        private Category category;

        @NotNull(message = "멘토 학번은 필수 입력값 입니다.")
        @Min(value = 10000000, message = "멘토 학번은 8자리 숫자여야 합니다.")
        @Max(value = 99999999, message = "멘토 학번은 8자리 숫자여야 합니다.")
        private Long mentorStudentNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudyGroupResponseDto {

        private Long studyId;

        private String name;

        private String description;

        private Category category;

        private String studyImage;

        private int currentParticipants;

        private String mentorName;

        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class StudyGroupUpdateDto {

        private String name;

        private String content;

        private Category category;

        @Min(value = 10000000, message = "멘토 학번은 8자리 숫자여야 합니다.")
        @Max(value = 99999999, message = "멘토 학번은 8자리 숫자여야 합니다.")
        private Long mentorStudentNumber;
    }
}
