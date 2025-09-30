package com.mjsec.lms.dto;

import com.mjsec.lms.type.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@Builder
public class DetailSubmissionResponse {

    private Long submissionId;

    private String content;

    private String password;

    private String creatorName;

    private String feedback;

    private SubmissionStatus status;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;
}
