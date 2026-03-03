package com.mjsec.lms.admin.dto;

import com.mjsec.lms.studygroup.domain.type.StudyStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllStudyGroupDto {

    private Long studyGroupId;

    private String name;

    private String category;

    private String studyImage;

    private String generation;

    private StudyStatus status;
}
