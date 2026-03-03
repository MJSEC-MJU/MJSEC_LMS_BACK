package com.mjsec.lms.announcement.dto;

import com.mjsec.lms.announcement.domain.type.AnnouncementRole;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponseDto {

    private Long announcementId;

    private Long userId;

    private String title;

    private String content;

    private AnnouncementRole type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
