package com.mjsec.lms.dto;

import com.mjsec.lms.type.StudyStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenteeStudyGroupDto {

    private Long studyGroupId;

    private String name;

    private String category;

    private String studyImage;

    private StudyStatus status;
}
