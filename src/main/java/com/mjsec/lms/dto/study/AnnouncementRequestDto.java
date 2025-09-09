package com.mjsec.lms.dto.study;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import com.mjsec.lms.type.AnnouncementRole;

@ Getter
public class AnnouncementRequestDto {

    private String title;

    private String content;

    @NotNull(message = "type은 필수입니다.")
    private AnnouncementRole type;
}
