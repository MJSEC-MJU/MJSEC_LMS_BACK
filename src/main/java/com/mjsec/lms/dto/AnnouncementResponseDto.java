package com.mjsec.lms.dto;

import com.mjsec.lms.type.AnnouncementType;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponseDto {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private AnnouncementType type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
