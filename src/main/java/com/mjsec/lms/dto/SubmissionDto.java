package com.mjsec.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//과제 제출 Dto
@Data
public class SubmissionDto {

    @NotBlank(message = "과제 내용을 입력해주세요!")
    private String content;

    private String password;
}