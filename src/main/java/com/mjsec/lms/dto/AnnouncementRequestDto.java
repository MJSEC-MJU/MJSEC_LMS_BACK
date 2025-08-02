package com.mjsec.lms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.time.LocalDateTime;
import com.mjsec.lms.type.AnnouncementRole;

@ Getter
public class AnnouncementRequestDto {

    private String title;

    private String content;

    @NotNull(message = "type은 필수입니다.")
    private AnnouncementRole type;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
