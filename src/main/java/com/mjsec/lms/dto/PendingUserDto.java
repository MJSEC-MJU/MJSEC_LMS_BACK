package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PendingUserDto {

    private Long studentNumber;

    private String name;

    private String email;

    private String phoneNumber;

    private LocalDateTime createdAt;
}
