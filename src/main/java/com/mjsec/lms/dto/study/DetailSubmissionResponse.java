package com.mjsec.lms.dto.study;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
