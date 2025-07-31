package com.mjsec.lms.mapper;

import com.mjsec.lms.domain.Announcement;
import com.mjsec.lms.dto.AnnouncementRequestDto;
import com.mjsec.lms.dto.AnnouncementResponseDto;

public class AnnouncementMapper {
    public static Announcement toEntity(AnnouncementRequestDto dto,Long userId) {
        return Announcement.builder()
                .userId(userId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .type(dto.getType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }
    public static AnnouncementResponseDto toDto(Announcement entity) {
        return AnnouncementResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}
