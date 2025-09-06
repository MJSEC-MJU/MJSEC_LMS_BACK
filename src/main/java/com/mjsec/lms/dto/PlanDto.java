package com.mjsec.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlanDto {

    @NotBlank(message = "제목을 입력해주세요!")
    private String title;

    @NotBlank(message = "내용을 입력해주세요!")
    private String content;

    @NotNull(message = "과제 여부는 꼭 체크해야 합니다!")
    private boolean hasAssignment;

    @NotNull(message = "시작일자는 필수입니다!")
    private LocalDateTime startDate;

    @NotNull(message = "종료일자는 필수입니다!")
    private LocalDateTime endDate;
}
