package com.mjsec.lms.studygroup.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyGroupPutResponse {

    private Long studyId;

    private String name;

    private String content;

    private String generation;

    private String studyImage;
}
