package com.mjsec.lms.assignment.dto;

import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DetailSubmissionResponse {

    private Long submissionId;

    private String content;

    private String password;

    private String creatorName;

    private String feedback;

    private SubmissionStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
