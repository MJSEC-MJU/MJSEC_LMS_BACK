package com.mjsec.lms.dto.study;

import com.mjsec.lms.type.AnnouncementRole;
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
