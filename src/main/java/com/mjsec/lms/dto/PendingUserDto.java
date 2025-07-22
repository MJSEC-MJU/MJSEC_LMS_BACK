package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingUserDto {

    private Long studentNumber;

    private String name;

    private String email;

    private String phoneNumber;
}
