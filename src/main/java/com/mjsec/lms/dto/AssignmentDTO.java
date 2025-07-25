package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentDTO {

    private String title;

    private String content;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

}
