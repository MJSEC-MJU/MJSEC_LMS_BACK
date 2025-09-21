package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyGroupSummaryDto {

    private Long studyGroupId;

    private String name;

    private String category;

    private String generation;

    private String studyImage;
}