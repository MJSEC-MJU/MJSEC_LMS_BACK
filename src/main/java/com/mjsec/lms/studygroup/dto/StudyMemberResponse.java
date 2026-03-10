package com.mjsec.lms.studygroup.dto;

import com.mjsec.lms.studygroup.domain.type.GroupMemberRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyMemberResponse {

    private Long userId;

    private Long studentNumber;

    private String name;

    private String email;

    private GroupMemberRole role;

    private String ProfileImage;
}
