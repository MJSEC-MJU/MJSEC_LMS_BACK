package com.mjsec.lms.dto.study;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SimpleStudyActivityResponse {

    private Long activityId;

    private String title;

    private String week;

    private String imageUrl;

    private LocalDateTime createdAt;
}
