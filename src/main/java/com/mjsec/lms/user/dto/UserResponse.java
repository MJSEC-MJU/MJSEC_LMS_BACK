package com.mjsec.lms.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.mjsec.lms.studygroup.dto.StudyGroupSummaryDto;

@Data
@Builder
public class UserResponse {

    private Long userId;

    private String name;

    private Long studentNumber;

    private String email;

    private String phoneNumber;

    private String profileImage;

    private LocalDateTime createdAt;

    private List<StudyGroupSummaryDto> studyGroups;
}