package com.mjsec.lms.studygroup.dto;

import com.mjsec.lms.assignment.domain.type.Category;
import jakarta.validation.constraints.*;
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

        @NotBlank(message = "기수 정보는 필수 입력값 입니다.")
        @Pattern(regexp = "^[1-9]\\d*(\\.5)?기$", message = "기수는 '1기', '1.5기', '2기' 형식으로 입력해주세요.")
        private String generation;
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

        private String generation;

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

        @Pattern(regexp = "^[1-9]\\d*(\\.5)?기$", message = "기수는 '1기', '1.5기', '2기' 형식으로 입력해주세요.")
        private String generation;
    }
}
