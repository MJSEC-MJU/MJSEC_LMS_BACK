package com.mjsec.lms.service;

import com.mjsec.lms.domain.PendingUser;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.PendingUserDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupRequestDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupUpdateDto;
import com.mjsec.lms.dto.UserAdminResponseDto;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.AnnouncementRepository;
import com.mjsec.lms.repository.AttendanceRepository;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.PendingUserRepository;
import com.mjsec.lms.repository.PlanCommentRepository;
import com.mjsec.lms.repository.PlanRepository;
import com.mjsec.lms.repository.StudyActivityRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.SubmissionRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.UserRole;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class AdminService {

    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final WikiService wikiService;
    private final PlanRepository planRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final AttendanceRepository attendanceRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final PlanCommentRepository planCommentRepository;
    private final AnnouncementRepository announcementRepository;
    private final FileService fileService;


    public AdminService(PendingUserRepository pendingUserRepository, UserRepository userRepository,
                        StudyGroupRepository studyGroupRepository, PlanRepository planRepository,
                        StudyActivityRepository studyActivityRepository, AttendanceRepository attendanceRepository,
                        GroupMemberRepository groupMemberRepository, SubmissionRepository submissionRepository,
                        PlanCommentRepository planCommentRepository, AnnouncementRepository announcementRepository,
                        WikiService wikiService, FileService fileService) {

        this.pendingUserRepository = pendingUserRepository;
        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.wikiService = wikiService;
        this.planRepository = planRepository;
        this.studyActivityRepository = studyActivityRepository;
        this.attendanceRepository = attendanceRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.submissionRepository = submissionRepository;
        this.planCommentRepository = planCommentRepository;
        this.announcementRepository = announcementRepository;
        this.fileService = fileService;
    }

    /**
     * 회원가입 승인 대기자 목록을 반환하는 메소드
     * @return List<PendingUserDto> (각 PendingUserDto는 학번, 이름, 이메일, 전화번호 정보를 담음)
     */
    public List<PendingUserDto> getAllPendingUser() {

        log.info("Getting all pending users");

        List<PendingUser> allPendingUsers = pendingUserRepository.findAll();

        return allPendingUsers.stream()
                .map(user -> PendingUserDto.builder()
                        .studentNumber(user.getStudentNumber())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .toList();
    }

    /**
     * 회원가입 승인을 위한 메소드
     * @param studentNumber 학번
     */
    @Transactional
    public void approveRegister(Long studentNumber){

        log.info("Approve registration for {}", studentNumber);

        PendingUser pendingUser = pendingUserRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        if (userRepository.existsByStudentNumber(pendingUser.getStudentNumber()) ||
                userRepository.existsByEmail(pendingUser.getEmail())) {
            log.warn("Already existing user: {}", studentNumber);
            throw new RestApiException(ErrorCode.ALREADY_REGISTERED_USER);
        }

        User user = User.builder()
                .studentNumber(pendingUser.getStudentNumber())
                .password(pendingUser.getPassword())
                .name(pendingUser.getName())
                .email(pendingUser.getEmail())
                .phoneNumber(pendingUser.getPhoneNumber())
                .role(UserRole.ROLE_USER)
                .build();

        userRepository.save(user);

        try {
            String wikiPassword = wikiService.extractPasswordFromEmail(pendingUser.getEmail());
            boolean wikiCreated = wikiService.createWikiUser(
                    pendingUser.getEmail(),
                    pendingUser.getName(),
                    wikiPassword
            );

            if (wikiCreated) {
                log.info("Successfully created Wiki account for user: {}", pendingUser.getEmail());
            } else {
                // Wiki 계정 생성 실패해도 LMS 계정은 유지 (로그만 남김)
                log.warn("Failed to create Wiki account for user: {} - LMS account still created",
                        pendingUser.getEmail());
            }
        } catch (Exception e) {
            // Wiki 서비스 오류가 LMS 계정 생성을 방해하지 않도록 예외 처리
            log.error("Error occurred while creating Wiki account for user: {} - {}",
                    pendingUser.getEmail(), e.getMessage());
        }

        pendingUserRepository.delete(pendingUser);
        log.info("Moved pending user to approved user: {}", user.getStudentNumber());
    }

    /**
     * 회원가입 승인 요청을 반려하는 메소드
     * @param studentNumber 학번
     */
    public void refuseRegister(Long studentNumber) {

        log.info("Refuse registration for {}", studentNumber);

        PendingUser pendingUser = pendingUserRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        pendingUserRepository.delete(pendingUser);
        log.info("Deleted pending user for registration refusal: {}", studentNumber);
    }

    /**
     * 스터디 그룹을 생성하는 메소드
     * @param requestDto 스터디 그룹명, 스터디 소개, 스터디 타입, 멘토 학번
     */
    public void createGroup(StudyGroupRequestDto requestDto) {

        User mentor = userRepository.findByStudentNumber(requestDto.getMentorStudentNumber())
                .orElseThrow(() -> new RestApiException(ErrorCode.INVALID_MENTOR_STUDENT_NUMBER));

        if(studyGroupRepository.existsByName(requestDto.getName())){
            throw new RestApiException(ErrorCode.STUDY_GROUP_ALREADY_EXIST);
        }

        StudyGroup studyGroup = StudyGroup.builder()
                .name(requestDto.getName())
                .category(requestDto.getCategory().name())
                .content(requestDto.getContent())
                .creator(mentor)
                .build();

        studyGroupRepository.save(studyGroup);
    }

    /**
     * 기존에 존재하는 스터디 그룹의 정보를 업데이트하는 메소드
     * @param name 스터디 그룹의 이름
     * @param studyImage 이미지 파일 (null 허용)
     * @param studyGroupUpdateDto name, content, category, mentorStudentNumber (null 허용)
     */
    public void updateGroup(String name, MultipartFile studyImage, StudyGroupUpdateDto studyGroupUpdateDto) {

        StudyGroup studyGroup = studyGroupRepository.findByName(name)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));

        if (studyImage != null && !studyImage.isEmpty()) {
            String imageUrl = fileService.uploadImage(studyImage);
            studyGroup.setStudyImage(imageUrl);
        }

        if (studyGroupUpdateDto.getName() != null && !studyGroupUpdateDto.getName().trim().isEmpty()) {
            if(studyGroupRepository.existsByName(studyGroupUpdateDto.getName())){
                throw new RestApiException(ErrorCode.STUDY_GROUP_ALREADY_EXIST);
            }
            else {
                studyGroup.setName(studyGroupUpdateDto.getName());
            }
        }

        if (studyGroupUpdateDto.getCategory() != null && !studyGroupUpdateDto.getCategory().name().trim().isEmpty()) {
            studyGroup.setCategory(studyGroupUpdateDto.getCategory().name());
        }

        if (studyGroupUpdateDto.getContent() != null && !studyGroupUpdateDto.getContent().trim().isEmpty()) {
            studyGroup.setContent(studyGroupUpdateDto.getContent());
        }

        if (studyGroupUpdateDto.getMentorStudentNumber() != null) {
            User mentor = userRepository.findByStudentNumber(studyGroupUpdateDto.getMentorStudentNumber())
                    .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

            studyGroup.setCreator(mentor);
        }

        studyGroupRepository.save(studyGroup);
    }

    /**
     * 모든 유저의 정보를 반환하는 메소드 (user_id, 학번, 이름, 이메일)
     * @return List<UserAdminResponseDto>
     */
    public List<UserAdminResponseDto> getAllUsersForAdmin() {

        return userRepository.findAll().stream()
                .map(user -> UserAdminResponseDto.builder()
                        .userId(user.getUserId())
                        .studentNumber(user.getStudentNumber())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .toList();
    }


    /**
     * 사용자와 모든 연관 데이터를 삭제하는 메소드
     * 외래키 제약조건을 고려하여 하위 데이터부터 상위 데이터 순으로 삭제
     *
     * @param userId 삭제할 사용자 ID
     * @throws RestApiException 사용자를 찾을 수 없거나 삭제 중 오류 발생 시
     */
    @Transactional
    public void deleteUser(Long userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        log.info("사용자 삭제 시도 - userId: {}, userName: {}", userId, user.getName());

        try {
            log.debug("1. 사용자 직접 관련 데이터 삭제 시작");

            submissionRepository.deleteBySubmitter(user);
            log.debug("- 과제 제출물 삭제 완료");

            planCommentRepository.deleteByAuthor(user);
            log.debug("- 댓글 삭제 완료");

            attendanceRepository.deleteByUser(user);
            log.debug("- 출석 정보 삭제 완료");

            groupMemberRepository.deleteByUser(user);
            log.debug("- 그룹 탈퇴 완료");

            announcementRepository.deleteByCreator(user);
            log.debug("- 공지사항 삭제 완료");

            log.debug("2. Plan 관련 데이터 삭제 시작");

            submissionRepository.deleteByPlanCreator(user);
            log.debug("- Plan에 의한 제출물 삭제 완료");

            planCommentRepository.deleteByPlanCreator(user);
            log.debug("- Plan 댓글 삭제 완료");

            planRepository.deleteByCreator(user);
            log.debug("- Plan 삭제 완료");

            log.debug("3. 스터디 활동 관련 데이터 삭제 시작");

            attendanceRepository.deleteByStudyActivityCreator(user);
            log.debug("- 출석 정보 삭제 완료");

            studyActivityRepository.deleteByCreator(user);
            log.debug("- 스터디 활동 삭제 완료");

            log.debug("4. 스터디 그룹 관련 데이터 삭제 시작");

            submissionRepository.deleteByStudyGroupCreator(user);
            log.debug("- 스터디 제출물 삭제 완료");

            planCommentRepository.deleteByStudyGroupCreator(user);
            log.debug("- 스터디 그룹에서 작성한 댓글 삭제 완료");

            attendanceRepository.deleteByStudyGroupCreator(user);
            log.debug("- 출석 정보 삭제 완료");

            groupMemberRepository.deleteByStudyGroupCreator(user);
            log.debug("- 모든 그룹 탈퇴");

            planRepository.deleteByStudyGroupCreator(user);
            log.debug("- Plan 삭제 완료");

            studyActivityRepository.deleteByStudyGroupCreator(user);
            log.debug("- 멘토와 관련된 모든 스터디 활동 정보 삭제 완료");

            studyGroupRepository.deleteByCreator(user);
            log.debug("- 스터디 그룹 삭제 완료");

            log.debug("5. 사용자 엔티티 삭제");
            userRepository.delete(user);

            log.info("사용자 삭제 완료 - userId: {}", userId);

        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RestApiException(ErrorCode.USER_DELETE_FAILED);
        }
    }
}
