package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlanResponse {
    private Long planId;

    private String title;

    private String content;

    private boolean hasAssignment;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime createdAt;
}
