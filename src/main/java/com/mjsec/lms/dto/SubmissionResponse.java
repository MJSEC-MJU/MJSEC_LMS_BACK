package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionResponse {
    private Long submissionId;

    private String content;

    private String creatorName;

    private LocalDateTime createdAt;
}
