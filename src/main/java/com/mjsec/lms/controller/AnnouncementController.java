package com.mjsec.lms.controller;

import com.mjsec.lms.dto.AnnouncementRequestDto;
import com.mjsec.lms.dto.AnnouncementResponseDto;
import com.mjsec.lms.service.AnnouncementService;
import com.mjsec.lms.type.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping("/announcement")
    public ResponseEntity<ResponseMessage<AnnouncementResponseDto>> createAnnouncement(
            @RequestBody AnnouncementRequestDto requestDto,
            Authentication authentication // Spring Security가 주입
    ) {
        // JwtFilter에서 설정한 userId를 가져온다고 가정
        Long currentUserId = (Long) authentication.getPrincipal();

        AnnouncementResponseDto responseDto = announcementService.createAnnouncement(requestDto, currentUserId);

        return ResponseEntity.ok(new ResponseMessage<>(responseDto));
    }
}