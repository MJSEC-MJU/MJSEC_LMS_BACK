package com.mjsec.lms.announcement.controller;

import com.mjsec.lms.announcement.domain.Announcement;
import com.mjsec.lms.announcement.dto.AnnouncementRequestDto;
import com.mjsec.lms.announcement.dto.AnnouncementResponseDto;
import com.mjsec.lms.common.dto.SuccessResponse;
import com.mjsec.lms.announcement.dto.AnnouncementMapper;
import com.mjsec.lms.announcement.service.AnnouncementService;
import com.mjsec.lms.common.type.ResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/create-announcement")
    public ResponseEntity<SuccessResponse<AnnouncementResponseDto>> createAnnouncement(
            @RequestBody AnnouncementRequestDto dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        Announcement saved = announcementService.createAnnouncement(dto, currentUserStudentNumber);

        AnnouncementResponseDto responseDto = AnnouncementMapper.toDto(saved);
        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.POST_ANNOUNCEMENT_SUCCESS,
                        responseDto
                )
        );
    }

    @GetMapping("/announcements")
    public ResponseEntity<SuccessResponse<List<AnnouncementResponseDto>>> getAnnouncements(Authentication authentication) {

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        List<AnnouncementResponseDto> announcements = announcementService.getAnnouncements(currentUserStudentNumber);
        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.GET_ANNOUNCEMENT_SUCCESS,
                        announcements
                ));
    }

    @GetMapping("/announcement/{announcementId}")
    public ResponseEntity<SuccessResponse<AnnouncementResponseDto>> getAnnouncementDetails(
            @PathVariable Long announcementId, Authentication authentication) {

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        AnnouncementResponseDto dto = announcementService.getAnnouncementDetail(announcementId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.DETAIL_ANNOUNCEMENT_SUCCESS,
                        dto
                )
        );
    }

    @PutMapping("/announcements/{announcementId}")
    public ResponseEntity<SuccessResponse<AnnouncementResponseDto>> updateAnnouncement(
            @PathVariable Long announcementId,
            @RequestBody AnnouncementRequestDto requestDto,
            Authentication authentication
    ){
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        AnnouncementResponseDto updated = announcementService.updateAnnouncement(announcementId,  requestDto,currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(ResponseMessage.UPDATE_ANNOUNCEMENT_SUCCESS,updated)
        );
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ResponseEntity<SuccessResponse> deleteAnnouncement(
            @PathVariable Long announcementId,
            Authentication authentication){

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        announcementService.deleteAnnouncement(announcementId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(ResponseMessage.DELETE_ANNOUNCEMENT_SUCCESS)
        );
    }




}


