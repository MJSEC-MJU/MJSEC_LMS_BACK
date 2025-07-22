package com.mjsec.lms.dto;

import lombok.Data;

@Data
public class PendingUserDto {

    private Long studentNumber;

    private String name;

    private String email;

    private String phoneNumber;
}
