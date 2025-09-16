package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyMemberWarnResponse {

    private Long userId;

    private Long studentNumber;

    private String name;

    private String email;

    private String ProfileImage;

    private Integer warn;
}
