package com.mjsec.lms.announcement.dto;

import com.mjsec.lms.announcement.domain.Announcement;
import com.mjsec.lms.user.domain.User;

public class AnnouncementMapper {
    public static Announcement toEntity(AnnouncementRequestDto dto, User user) {
        return Announcement.builder()
                .creator(user)
                .title(dto.getTitle())
                .content(dto.getContent())
                .type(dto.getType())
                .build();
    }
    public static AnnouncementResponseDto toDto(Announcement entity) {
        return AnnouncementResponseDto.builder()
                .announcementId(entity.getAnnouncementId())
                .userId(entity.getCreator() != null ? entity.getCreator().getUserId() : null) // User 객체에서 userId 추출ﬁ
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
