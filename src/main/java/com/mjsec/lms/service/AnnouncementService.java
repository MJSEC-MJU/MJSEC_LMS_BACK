package com.mjsec.lms.service;

import com.mjsec.lms.domain.Announcement;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AnnouncementRequestDto;
import com.mjsec.lms.dto.AnnouncementResponseDto;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.mapper.AnnouncementMapper;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.UserRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository) {

        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    // 공지사항 생성하기
    public Announcement  createAnnouncement(AnnouncementRequestDto dto, Long currentUserStudentNumber) {

        //유저가 맞는지 확인 + 관리자인지 확인
        User user = validateUser(currentUserStudentNumber);
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new RestApiException(ErrorCode.UNAUTHORIZED_ROLE);
        }
        if (dto.getType() == null) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_TYPE_REQUIRED);
        }

        Announcement announcement= Announcement.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .type(dto.getType())
                .createdAt(LocalDateTime.now())
                .creator(user)
                .build();
                return announcementRepository.save(announcement);
    }

    //전체 공지사항 목록 반환하기
    public List<AnnouncementResponseDto> getAnnouncements(Long currentUserStudentNumber ) {

        //전체 공지사항을  조회하려는 유저 확인하기
        User user = userRepository.findByStudentNumber(currentUserStudentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

       //공지사항이 존재하는지 확인하기
        List<Announcement> announcements = announcementRepository.findAll();
        if(announcements.isEmpty()) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }

        return announcements.stream()
                .map((AnnouncementMapper::toDto))
                .collect(Collectors.toList());

    }

    //세부 공지사항 내용을 반환하기
    public AnnouncementResponseDto  getAnnouncementDetail(Long announcementId,Long currentUserStudentNumber){

        //유저 존재 확인
        User user = userRepository.findByStudentNumber(currentUserStudentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        //공지사항 존재 확인
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        return AnnouncementMapper.toDto(announcement);
    }

    private User validateUser(Long studentNumber) {
        return userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));
    }

}