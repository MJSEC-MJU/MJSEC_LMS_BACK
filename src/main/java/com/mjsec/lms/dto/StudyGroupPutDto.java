package com.mjsec.lms.dto;

import com.mjsec.lms.type.StudyStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyGroupPutDto {

    private String name;

    private String content;

    private String studyImage;

}
