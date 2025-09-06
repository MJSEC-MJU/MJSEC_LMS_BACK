package com.mjsec.lms.dto;

import com.mjsec.lms.type.GroupMemberRole;
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
