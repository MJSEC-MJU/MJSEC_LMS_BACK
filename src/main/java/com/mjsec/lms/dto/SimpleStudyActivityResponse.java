package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SimpleStudyActivityResponse {

    private Long activityId;

    private String title;

    private String week;

    private List<String> imageUrls;

    private LocalDateTime createdAt;
}
