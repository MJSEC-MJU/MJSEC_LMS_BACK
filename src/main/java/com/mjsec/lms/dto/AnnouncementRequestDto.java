package com.mjsec.lms.dto;

import lombok.Getter;
import java.time.LocalDateTime;
import com.mjsec.lms.type.AnnouncementType;

@ Getter
public class AnnouncementRequestDto {
    private String title;
    private String content;
    private   AnnouncementType type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
