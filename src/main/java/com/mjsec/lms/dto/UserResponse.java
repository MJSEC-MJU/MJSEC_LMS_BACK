package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long userId;

    private Long studentNumber;

    private String email;

    private String phoneNumber;

    private String profileImage;

    private LocalDateTime createdAt;
}
