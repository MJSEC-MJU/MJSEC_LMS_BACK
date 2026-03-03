package com.mjsec.lms.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAdminResponseDto {

    private Long userId;

    private Long studentNumber;

    private String name;

    private String email;
}

