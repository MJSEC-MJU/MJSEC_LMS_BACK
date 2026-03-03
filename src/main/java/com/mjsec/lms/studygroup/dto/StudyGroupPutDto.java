package com.mjsec.lms.studygroup.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyGroupPutDto {

    private String name;

    private String content;

    private String studyImage;

}
