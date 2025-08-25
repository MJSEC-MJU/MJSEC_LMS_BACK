package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentCommentResponse {

    private Long commentId;

    private Long assignmentId;

    private String content;

    private String creatorName;

    private LocalDateTime createdAt;
}
