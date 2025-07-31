package com.mjsec.lms.controller;

import com.mjsec.lms.domain.Announcement;
import com.mjsec.lms.dto.AnnouncementRequestDto;
import com.mjsec.lms.dto.AnnouncementResponseDto;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.mapper.AnnouncementMapper;
import com.mjsec.lms.service.AnnouncementService;
import com.mjsec.lms.type.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/announcement")
    public ResponseEntity<SuccessResponse<AnnouncementResponseDto>> createAnnouncement(
            @RequestBody AnnouncementRequestDto dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        Announcement saved = announcementService.createAnnouncement(dto, currentUserStudentNumber);

        AnnouncementResponseDto responseDto = AnnouncementMapper.toDto(saved);
        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ANNOUNCEMENT_SUCCESS,
                        responseDto
                ));

    }




}

