package com.mjsec.lms.announcement.service;

import com.mjsec.lms.announcement.domain.Announcement;
import com.mjsec.lms.user.domain.User;
import com.mjsec.lms.announcement.dto.AnnouncementRequestDto;
import com.mjsec.lms.announcement.dto.AnnouncementResponseDto;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.announcement.dto.AnnouncementMapper;
import com.mjsec.lms.announcement.repository.AnnouncementRepository;
import com.mjsec.lms.user.repository.UserRepository;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.user.domain.type.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
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

        //유저가 맞는지 확인
        User user = validateUser(currentUserStudentNumber);

        //관리자인지 확인
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_UNAUTHORIZED_ROLE);
        }

        //제목이 비어있는지 확인
        if(dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_TITLE_REQUIRED);
        }

        //내용이 비어있는지 확인
        if(dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_CONTENT_REQUIRED);
        }

        //타입이 비어있는지 확인
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
        User user = validateUser(currentUserStudentNumber);

        //공지사항이 존재하는지 확인하기
        List<Announcement> announcements = announcementRepository.findAll();
        if (announcements.isEmpty()) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }

        return announcements.stream()
                .sorted(Comparator.comparing(Announcement::getAnnouncementId).reversed())
                .map(AnnouncementMapper::toDto)
                .collect(Collectors.toList());
    }

    //세부 공지사항 내용을 반환하기
    public AnnouncementResponseDto  getAnnouncementDetail(Long announcementId,Long currentUserStudentNumber){

        //유저 존재 확인
        User user = validateUser(currentUserStudentNumber);

        //공지사항 존재 확인
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        return AnnouncementMapper.toDto(announcement);
    }

    //공지사항 수정하기
    public AnnouncementResponseDto updateAnnouncement(Long announcementId, AnnouncementRequestDto dto,Long currentUserStudentNumber) {

        // 유저 확인
        User user = validateUser(currentUserStudentNumber);

        //관리자 권한 확인
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_UNAUTHORIZED_ROLE);
        }

        // 공지사항 확인
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        //작성자 본인 확인
        if(!announcement.getCreator().getUserId().equals(user.getUserId())) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_FORBIDDEN);
        }

        //데이터가 null이 아닌 경우에만 업데이트
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            announcement.setTitle(dto.getTitle());
        }

        if (dto.getContent() != null&& !dto.getContent().trim().isEmpty()) {
            announcement.setContent(dto.getContent());
        }

        if (dto.getType() != null) {
            announcement.setType(dto.getType());
        }

        announcement.setUpdatedAt(LocalDateTime.now());

        Announcement saved = announcementRepository.save(announcement);
        return AnnouncementMapper.toDto(saved);
    }

    //공지사항 삭제하기
    @Transactional
    public void  deleteAnnouncement(Long announcementId, Long currentUserStudentNumber) {

        //유저 권한 확인
        User user = validateUser(currentUserStudentNumber);

        // 관리자 권한 확인
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_UNAUTHORIZED_ROLE);
        }

        // 조회할 수 있는 공지사항인지 확인
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ANNOUNCEMENT_NOT_FOUND)
                );

        //작성자 본인이 맞는지 확인
        if (!announcement.getCreator().getUserId().equals(user.getUserId())) {
            throw new RestApiException(ErrorCode.ANNOUNCEMENT_FORBIDDEN);
        }

        announcementRepository.delete(announcement);
    }

    private User validateUser(Long studentNumber) {
        return userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));
    }
}