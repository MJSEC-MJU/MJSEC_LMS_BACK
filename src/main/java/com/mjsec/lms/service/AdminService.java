package com.mjsec.lms.service;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.PendingUser;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AllStudyGroupDto;
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
import com.mjsec.lms.type.GroupMemberRole;
import com.mjsec.lms.type.UserRole;
import com.mjsec.lms.type.StudyStatus;
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
                        .createdAt(user.getCreatedAt())
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
     * 모든 스터디 그룹의 정보를 반환하는 메소드 (study group Id, 이름, 카테고리, 스터디 그룹 대표 이미지, 스터디 상태만 반환)
     * @return List<AllStudyGroupDto>
     */
    public List<AllStudyGroupDto> getAllGroups() {

        log.info("Getting All Groups Info");

        List<AllStudyGroupDto> groups = studyGroupRepository.findAll().stream()
                .map(studyGroup -> {
                    return AllStudyGroupDto.builder()
                            .studyGroupId(studyGroup.getStudyId())
                            .name(studyGroup.getName())
                            .category(studyGroup.getCategory())
                            .studyImage(studyGroup.getStudyImage())
                            .generation(studyGroup.getGeneration())
                            .status(studyGroup.getStatus())
                            .build();
                })
                .toList();

        log.info("{} Groups returned", groups.size());

        return groups;
    }

    /**
     * 사용 중인 스터디 이름인지 체크하는 메소드
     * @param name 새로운 스터디 이름
     * @return true / false
     */
    public boolean checkGroupName(String name) {

        if(studyGroupRepository.existsByName(name)){
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * 스터디 그룹을 생성하는 메소드
     * @param requestDto 스터디 그룹명, 스터디 소개, 스터디 타입, 멘토 학번
     */
    @Transactional
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
                .generation(requestDto.getGeneration())
                .creator(mentor)
                .status(StudyStatus.ACTIVE)
                .build();

        studyGroupRepository.save(studyGroup);

        GroupMember groupMentor = GroupMember.builder()
                .role(GroupMemberRole.MENTOR)
                .user(mentor)
                .studyGroup(studyGroup)
                .build();

        groupMemberRepository.save(groupMentor);
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

        if(studyGroupUpdateDto != null) {
            if (studyGroupUpdateDto.getName() != null && !studyGroupUpdateDto.getName().trim().isEmpty()) {
                if (studyGroupRepository.existsByName(studyGroupUpdateDto.getName())) {
                    throw new RestApiException(ErrorCode.STUDY_GROUP_ALREADY_EXIST);
                } else {
                    studyGroup.setName(studyGroupUpdateDto.getName());
                }
            }

            if (studyGroupUpdateDto.getCategory() != null && !studyGroupUpdateDto.getCategory().name().trim()
                    .isEmpty()) {
                studyGroup.setCategory(studyGroupUpdateDto.getCategory().name());
            }

            if (studyGroupUpdateDto.getContent() != null && !studyGroupUpdateDto.getContent().trim().isEmpty()) {
                studyGroup.setContent(studyGroupUpdateDto.getContent());
            }

            if (studyGroupUpdateDto.getGeneration() != null && !studyGroupUpdateDto.getGeneration().trim().isEmpty()) {
                studyGroup.setGeneration(studyGroupUpdateDto.getGeneration());
            }

            if (studyGroupUpdateDto.getMentorStudentNumber() != null) {
                User mentor = userRepository.findByStudentNumber(studyGroupUpdateDto.getMentorStudentNumber())
                        .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

                studyGroup.setCreator(mentor);
            }
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
            if (user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
                try {
                    fileService.deleteImage(user.getProfileImage());
                    log.info("User profile image deleted successfully: {}", user.getProfileImage());
                } catch (Exception e) {
                    log.warn("Failed to delete user profile image: {}, but continuing with user deletion",
                            user.getProfileImage(), e);
                }
            }

            List<StudyGroup> userCreatedGroups = studyGroupRepository.findByCreatorUserId(userId);

            for (StudyGroup studyGroup : userCreatedGroups) {
                if (studyGroup.getStudyImage() != null && !studyGroup.getStudyImage().trim().isEmpty()) {
                    try {
                        fileService.deleteImage(studyGroup.getStudyImage());
                        log.info("Study group image deleted successfully for group {}: {}",
                                studyGroup.getStudyId(), studyGroup.getStudyImage());
                    } catch (Exception e) {
                        log.warn("Failed to delete study group image for group {}: {}, but continuing with deletion",
                                studyGroup.getStudyId(), studyGroup.getStudyImage(), e);
                    }
                }
            }

            submissionRepository.deleteBySubmitter(user);

            planCommentRepository.deleteByAuthor(user);

            attendanceRepository.deleteByUser(user);

            groupMemberRepository.deleteByUser(user);

            announcementRepository.deleteByCreator(user);

            submissionRepository.deleteByPlanCreator(user);

            planCommentRepository.deleteByPlanCreator(user);

            planRepository.deleteByCreator(user);

            attendanceRepository.deleteByStudyActivityCreator(user);

            studyActivityRepository.deleteByCreator(user);

            submissionRepository.deleteByStudyGroupCreator(user);

            planCommentRepository.deleteByStudyGroupCreator(user);

            attendanceRepository.deleteByStudyGroupCreator(user);

            groupMemberRepository.deleteByStudyGroupCreator(user);

            planRepository.deleteByStudyGroupCreator(user);

            studyActivityRepository.deleteByStudyGroupCreator(user);

            studyGroupRepository.deleteByCreator(user);

            userRepository.delete(user);

            log.info("사용자 삭제 완료");
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RestApiException(ErrorCode.USER_DELETE_FAILED);
        }
    }

    /**
     * 스터디 그룹의 상태를 변경시키는 메소드 (ACTIVATE <-> INACTIVE)
     * @param groupId
     */
    public void updateGroupStatus(Long groupId) {

        log.info("Updating Study Group Status : group_id {}", groupId);

        StudyGroup studyGroup = studyGroupRepository.findByStudyId(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));

        StudyStatus currentStatus = studyGroup.getStatus();
        StudyStatus newStatus = (currentStatus == StudyStatus.ACTIVE) ? StudyStatus.INACTIVE : StudyStatus.ACTIVE;
        studyGroup.setStatus(newStatus);

        studyGroupRepository.save(studyGroup);

        log.info("Study Group Status Successfully Updated : group_id {}", groupId);
    }
}
