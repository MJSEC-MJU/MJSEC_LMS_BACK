package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
//과제 상세 조회하기용
public class DetailAssignmentResponse {
    private Long assignmentId;

    private String title;

    private String content;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String creatorName;

    private int commentCount;

    private List<AssignmentCommentResponse> commentList;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
