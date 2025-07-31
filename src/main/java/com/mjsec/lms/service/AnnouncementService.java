package com.mjsec.lms.service;

import com.mjsec.lms.domain.Announcement;
import com.mjsec.lms.dto.AnnouncementRequestDto;
import com.mjsec.lms.dto.AnnouncementResponseDto;
import com.mjsec.lms.mapper.AnnouncementMapper;
import com.mjsec.lms.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementResponseDto createAnnouncement(AnnouncementRequestDto dto,Long userId) {
        Announcement announcement = AnnouncementMapper.toEntity(dto, userId);
        Announcement saved = announcementRepository.save(announcement);
        return AnnouncementMapper.toDto(saved);
    }
}
