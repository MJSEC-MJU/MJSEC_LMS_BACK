package com.mjsec.lms.assignment.dto;

import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionFeedbackDto {

    private String feedback;

    @NotNull(message = "과제 상태는 필수입니다.")
    private SubmissionStatus status;
}
