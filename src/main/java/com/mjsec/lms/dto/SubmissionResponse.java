package com.mjsec.lms.dto;

import com.mjsec.lms.type.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

//과제 제출 반환 Dto
@Data
@Builder
public class SubmissionResponse {

    private Long submissionId;

    private String content;

    private String creatorName;

    private SubmissionStatus status;

    private LocalDateTime createdAt;
}
