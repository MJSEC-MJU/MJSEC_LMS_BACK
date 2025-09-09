package com.mjsec.lms.dto.mentor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
//과제 상세 조회하기용
public class DetailPlanResponse {
    private Long planId;

    private String title;

    private String content;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean hasAssignment;

    private String creatorName;

    private int commentCount;

    private List<PlanCommentResponse> commentList;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
