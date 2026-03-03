package com.mjsec.lms.studygroup.service;

import com.mjsec.lms.user.domain.User;
import com.mjsec.lms.studygroup.domain.GroupMember;
import com.mjsec.lms.studygroup.domain.StudyGroup;
import com.mjsec.lms.studygroup.domain.StudyActivity;
import com.mjsec.lms.attendance.domain.Attendance;
import com.mjsec.lms.studygroup.dto.StudyMemberResponse;
import com.mjsec.lms.studygroup.dto.StudyMemberWarnResponse;
import com.mjsec.lms.studygroup.dto.StudyActivityDto;
import com.mjsec.lms.studygroup.dto.StudyActivityResponse;
import com.mjsec.lms.studygroup.dto.SimpleStudyActivityResponse;
import com.mjsec.lms.studygroup.dto.StudyGroupDetailDto;
import com.mjsec.lms.attendance.dto.StudyAttendanceDto;
import com.mjsec.lms.admin.dto.AllStudyGroupDto;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.studygroup.repository.StudyGroupRepository;
import com.mjsec.lms.studygroup.repository.StudyActivityRepository;
import com.mjsec.lms.user.repository.UserRepository;
import com.mjsec.lms.attendance.repository.AttendanceRepository;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.studygroup.domain.type.GroupMemberRole;
import com.mjsec.lms.common.util.JsonArrayUtils;
import com.mjsec.lms.common.util.ValidationUtils;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.mjsec.lms.media.service.FileService;

@Service
@Slf4j
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final ValidationUtils validationUtils;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final FileService fileService;
    private final JsonArrayUtils jsonArrayUtils;

    StudyGroupService(StudyGroupRepository studyGroupRepository,
                      ValidationUtils validationUtils,
                      StudyActivityRepository studyActivityRepository,
                      UserRepository userRepository,
                      AttendanceRepository attendanceRepository,
                      GroupMemberRepository groupMemberRepository,
                      FileService fileService,
                      JsonArrayUtils jsonArrayUtils) {

        this.studyGroupRepository = studyGroupRepository;
        this.validationUtils = validationUtils;
        this.studyActivityRepository = studyActivityRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.fileService = fileService;
        this.jsonArrayUtils = jsonArrayUtils;
    }

    //스터디 그룹 멤버 전체 반환
    @Transactional(readOnly = true)
    public List<StudyMemberResponse> getStudyMemberList(Long groupId, Long currentUserStudentNumber){
        log.info("getStudyMemberList called for group: {} by user: {}", groupId, currentUserStudentNumber);

        List<GroupMember> groupMemberList = groupMemberRepository.findByStudyGroup_StudyId(groupId);

        log.info("Found {} members in study group: {}", groupMemberList.size(), groupId);

        return groupMemberList.stream()
                .map(this::createStudyMemberResponse)
                .collect(Collectors.toList());
    }

    //스터디 그룹 멘티 멤버만 반환
    @Transactional(readOnly = true)
    public List<StudyMemberResponse> getStudyMenteeList(Long groupId, Long currentUserStudentNumber){
        log.info("getStudyMenteeList called for group: {} by user: {}", groupId, currentUserStudentNumber);

        // MENTEE 역할을 가진 멤버만 필터링하여 조회
        List<GroupMember> groupMenteeList = groupMemberRepository.findByStudyGroup_StudyIdAndRole(groupId, GroupMemberRole.MENTEE);

        log.info("Found {} mentees in study group: {}", groupMenteeList.size(), groupId);

        return groupMenteeList.stream()
                .map(this::createStudyMemberResponse)
                .collect(Collectors.toList());
    }

    //스터디 그룹 멘티 멤버 경고 반환
    @Transactional(readOnly = true)
    public List<StudyMemberWarnResponse> getStudyMemberWarnList(Long groupId, Long currentUserStudentNumber){

        log.info("getStudyMemberWarnList called!");

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);
         validationUtils.validateStudyGroup(groupId);

        List<GroupMember> groupMenteeList = groupMemberRepository.findByStudyGroup_StudyIdAndRole(groupId, GroupMemberRole.MENTEE);

        log.info("Found {} mentees in study group: {}", groupMenteeList.size(), groupId);

        return groupMenteeList.stream()
                .map(this::createStudyMemberWarnResponse)
                .collect(Collectors.toList());
    }

    //스터디 활동 글 + 출석체크 설정하기
    @Transactional
    public StudyActivityResponse createStudyActivity(Long groupId, Long currentUserStudentNumber,
                                                     StudyActivityDto studyActivityDto, List<MultipartFile> images) {

        log.info("createStudyActivity called for group: {} by user: {}", groupId, currentUserStudentNumber);

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        // 기존 검증들
        validationUtils.validateDuplicateWeekForCreate(studyGroup, studyActivityDto.getWeek());

        // 내용 검증 추가 (XSS/SQL Injection 방어)
        validationUtils.validateActivityContent(studyActivityDto.getTitle(), studyActivityDto.getContent());

        // 이미지가 있으면 업로드
        List<String> imageUrls = null;
        if (images != null && !images.isEmpty()) {
            imageUrls = fileService.uploadMultipleImages(images);
            log.info("Uploaded {} images for new study activity", imageUrls.size());
        }
        studyActivityDto.setImageUrls(imageUrls);

        StudyActivity savedStudyActivity = createStudyActivityData(groupId, studyActivityDto,user);
        List<Attendance> attendanceList = createAttendanceListData(savedStudyActivity, studyActivityDto);

        log.info("StudyActivity created successfully with ID: {}", savedStudyActivity.getActivityId());

        return createStudyActivityResponse(savedStudyActivity, attendanceList);
    }

    //스터디 활동 글 수정하기
    @Transactional
    public StudyActivityResponse updateStudyActivity(Long groupId, Long activityId,
                                                     Long currentUserStudentNumber, StudyActivityDto studyActivityDto, List<MultipartFile> images) {

        log.info("updateStudyActivity called for activity: {} in group: {} by user: {}",
                activityId, groupId, currentUserStudentNumber);

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);

        validationUtils.validateActivityBelongsToGroup(activityId, groupId); // 연관관계 검증
        StudyActivity studyActivity = validationUtils.validateStudyActivity(activityId);
        validationUtils.validateActivityOwnership(studyActivity, user.getUserId()); // 소유권 검증
        validationUtils.validateActivityContent(studyActivityDto.getTitle(), studyActivityDto.getContent()); // 내용 검증

        StudyGroup studyGroup = studyActivity.getStudyGroup();

        // 중복 주차 검증 (현재 활동 글 제외)
        validationUtils.validateDuplicateWeekForUpdate(studyGroup, studyActivityDto.getWeek(), activityId);

        // 이미지 처리 로직
        handleMultipleImageUpdate(studyActivity, images, studyActivityDto);

        // 다른 필드들 업데이트
        updateStudyActivityData(studyActivity, studyActivityDto);

        // 출석체크 데이터도 업데이트
        List<Attendance> updatedAttendanceList = updateAttendanceData(studyActivity, studyActivityDto);

        log.info("StudyActivity updated successfully: {}", activityId);

        return createStudyActivityResponse(studyActivity, updatedAttendanceList);
    }

    // 스터디 활동 글 전체 조회
    @Transactional(readOnly = true)
    public List<SimpleStudyActivityResponse> getStudyActivityList(Long groupId, Long currentUserStudentNumber){

        log.info("getStudyActivityList called for group: {} by user: {}", groupId, currentUserStudentNumber);

        // 기존 검증 유지 (이미 적절함 - 멤버십 체크 포함)
        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);

        List<StudyActivity> studyActivityList = studyActivityRepository.findAllByStudyGroupId(groupId);

        log.info("Found {} activities in study group: {}", studyActivityList.size(), groupId);

        return createSimpleStudyActivityList(studyActivityList);
    }

    // 스터디 활동 글 삭제
    @Transactional
    public void deleteStudyActivity(Long groupId, Long activityId, Long currentUserStudentNumber){

        log.info("deleteStudyActivity called for activity: {} in group: {} by user: {}",
                activityId, groupId, currentUserStudentNumber);

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);

        validationUtils.validateActivityBelongsToGroup(activityId, groupId); // 연관관계 검증
        StudyActivity studyActivity = validationUtils.validateStudyActivity(activityId);
        validationUtils.validateActivityOwnership(studyActivity, user.getUserId()); // 소유권 검증

        // 이미지가 있다면 파일 시스템에서 삭제
        List<String> imageUrls = getImageUrlsFromEntity(studyActivity);
        if (!imageUrls.isEmpty()) {
            try {
                fileService.deleteMultipleImages(imageUrls);
                log.info("Multiple image files deleted successfully: {} images", imageUrls.size());
            } catch (Exception e) {
                log.warn("Failed to delete some image files, but continuing with activity deletion", e);
            }
        }

        studyActivityRepository.delete(studyActivity);

        log.info("StudyActivity deleted successfully: {}", activityId);
    }

    //활동 글 상세 조회
    @Transactional(readOnly = true)
    public StudyActivityResponse getStudyActivity(Long groupId, Long activityId, Long currentUserStudentNumber){

        log.info("getStudyActivity called for activity: {} in group: {} by user: {}",
                activityId, groupId, currentUserStudentNumber);

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);
        validationUtils.validateGroupMembership(user, studyGroup);

        // 연관관계 검증 추가
        validationUtils.validateActivityBelongsToGroup(activityId, groupId);
        StudyActivity studyActivity = validationUtils.validateStudyActivity(activityId);

        log.info("Found study activity: {} with title: {}", activityId, studyActivity.getTitle());

        return createStudyActivityResponse(studyActivity, studyActivity.getAttendances());
    }

    // 특정 그룹 상세 정보 조회
    @Transactional(readOnly = true)
    public StudyGroupDetailDto getStudyGroupDetail(Long groupId, Long currentUserStudentNumber) {

        log.info("getStudyGroupDetail called for group: {} by user: {}", groupId, currentUserStudentNumber);

        // 기본 접근 권한 검증 (해당 그룹의 멤버인지 확인)
        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        // 그룹의 모든 멤버 조회
        List<GroupMember> allMembers = groupMemberRepository.findByStudyGroup_StudyId(groupId);

        // 멘토와 멘티 분리
        GroupMember mentor = allMembers.stream()
                .filter(member -> member.getRole() == GroupMemberRole.MENTOR)
                .findFirst()
                .orElse(null);

        List<GroupMember> mentees = allMembers.stream()
                .filter(member -> member.getRole() == GroupMemberRole.MENTEE)
                .toList();

        // 멘티 정보 DTO 리스트 생성
        List<StudyGroupDetailDto.MenteeInfo> menteeInfoList = mentees.stream()
                .map(this::createMenteeInfo)
                .collect(Collectors.toList());

        log.info("Found {} total members in study group: {} (1 mentor, {} mentees)",
                allMembers.size(), groupId, mentees.size());

        return createStudyGroupDetailDto(studyGroup, mentor, menteeInfoList);
    }

    // 관리자 권한으로 특정 그룹 상세 정보 조회
    @Transactional(readOnly = true)
    public StudyGroupDetailDto getStudyGroupDetailForAdmin(Long groupId) {

        log.info("getStudyGroupDetailForAdmin called for group: {}", groupId);

        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        // 그룹의 모든 멤버 조회
        List<GroupMember> allMembers = groupMemberRepository.findByStudyGroup_StudyId(groupId);

        // 멘토와 멘티 분리
        GroupMember mentor = allMembers.stream()
                .filter(member -> member.getRole() == GroupMemberRole.MENTOR)
                .findFirst()
                .orElse(null);

        List<GroupMember> mentees = allMembers.stream()
                .filter(member -> member.getRole() == GroupMemberRole.MENTEE)
                .toList();

        // 멘티 정보 DTO 리스트 생성
        List<StudyGroupDetailDto.MenteeInfo> menteeInfoList = mentees.stream()
                .map(this::createMenteeInfo)
                .collect(Collectors.toList());

        log.info("Found {} total members in study group: {} (1 mentor, {} mentees)",
                allMembers.size(), groupId, mentees.size());

        return createStudyGroupDetailDto(studyGroup, mentor, menteeInfoList);
    }

    /*
    그 외 Private 메서드
     */

    private void handleMultipleImageUpdate(StudyActivity studyActivity, List<MultipartFile> newImages, StudyActivityDto dto) {
        List<String> currentImageUrls = getImageUrlsFromEntity(studyActivity);

        // FileService의 updateMultipleImages 사용 - null이면 기존 이미지들 모두 삭제
        List<String> newImageUrls = fileService.updateMultipleImages(
                currentImageUrls,
                newImages,
                "StudyActivity",
                studyActivity.getActivityId()
        );

        // 엔티티에 새로운 이미지 URL들 설정 - JsonArrayUtils 사용
        setImageUrlsToEntity(studyActivity, newImageUrls);
        dto.setImageUrls(newImageUrls); // DTO에도 설정
        studyActivityRepository.save(studyActivity);

        log.info("Multiple images updated for activity {}: {} images",
                studyActivity.getActivityId(), newImageUrls.size());
    }

    // StudyActivity 엔티티에서 이미지 URL 리스트 추출
    private List<String> getImageUrlsFromEntity(StudyActivity studyActivity) {
        return jsonArrayUtils.parseStringArray(studyActivity.getImageUrls());
    }

    // StudyActivity 엔티티에 이미지 URL 리스트 설정
    private void setImageUrlsToEntity(StudyActivity studyActivity, List<String> imageUrls) {
        String jsonString = jsonArrayUtils.toStringArray(imageUrls);
        studyActivity.setImageUrls(jsonString);
    }

    // 출석체크 데이터 업데이트 메서드 (전체 삭제 후 재생성)
    private List<Attendance> updateAttendanceData(StudyActivity studyActivity, StudyActivityDto dto) {

        log.info("updateAttendanceData called for activityId: {}", studyActivity.getActivityId());

        // 출석체크 DTO 리스트가 있는 경우에만 업데이트
        if (dto.getStudyAttendanceDtoList() != null && !dto.getStudyAttendanceDtoList().isEmpty()) {

            // 기존 출석체크 데이터 삭제
            List<Attendance> existingAttendances = attendanceRepository.findByStudyActivity(studyActivity);
            if (!existingAttendances.isEmpty()) {
                attendanceRepository.deleteAll(existingAttendances);
                log.info("Deleted {} existing attendance records for activityId: {}",
                        existingAttendances.size(), studyActivity.getActivityId());
            }

            // 새로운 출석체크 데이터 생성 및 저장
            List<Attendance> newAttendances = dto.getStudyAttendanceDtoList().stream()
                    .map(attendanceDto -> Attendance.builder()
                            .user(userRepository.findByStudentNumber(attendanceDto.getStudentNumber()).orElseThrow())
                            .studyGroup(studyActivity.getStudyGroup())
                            .studyActivity(studyActivity)
                            .type(attendanceDto.getType())
                            .week(studyActivity.getWeek())
                            .attendanceDate(LocalDate.now())
                            .build())
                    .collect(Collectors.toList());

            List<Attendance> savedAttendances = attendanceRepository.saveAll(newAttendances);
            log.info("Saved {} new attendance records for activityId: {}",
                    savedAttendances.size(), studyActivity.getActivityId());

            return savedAttendances;
        } else {
            // 출석체크 DTO가 없으면 기존 출석체크 데이터 그대로 반환
            log.info("No attendance DTO provided, keeping existing attendance data");
            return attendanceRepository.findByStudyActivity(studyActivity);
        }
    }

    /*
    DTO 생성용 메서드
     */

    //Dto를 받아서 StudyActivity 저장하기
    public StudyActivity createStudyActivityData(Long groupId, StudyActivityDto studyActivityDto, User user){

        StudyActivity studyActivity = StudyActivity.builder()
                .title(studyActivityDto.getTitle())
                .content(studyActivityDto.getContent())
                .studyGroup(studyGroupRepository.findById(groupId).orElseThrow())
                .week(studyActivityDto.getWeek())
                .creator(user)
                .build();

        if (studyActivityDto.getImageUrls() != null) {
            setImageUrlsToEntity(studyActivity, studyActivityDto.getImageUrls());
        }

        return studyActivityRepository.save(studyActivity);
    }

    private void updateStudyActivityData(StudyActivity studyActivity, StudyActivityDto dto) {
        boolean isUpdated = false;

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            studyActivity.setTitle(dto.getTitle());
            isUpdated = true;
        }
        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            studyActivity.setContent(dto.getContent());
            isUpdated = true;
        }
        if (dto.getWeek() != null && !dto.getWeek().trim().isEmpty()) {
            studyActivity.setWeek(dto.getWeek());
            isUpdated = true;
        }

        if (isUpdated) {
            studyActivityRepository.save(studyActivity);
            log.info("StudyActivity data updated successfully: {}", studyActivity.getActivityId());
        }
    }

    //StudyMemberResponse Dto로 반환
    private StudyMemberResponse createStudyMemberResponse(GroupMember groupMember){

        return StudyMemberResponse.builder()
                .userId(groupMember.getUser().getUserId())
                .studentNumber(groupMember.getUser().getStudentNumber())
                .name(groupMember.getUser().getName())
                .role(groupMember.getRole())
                .email(groupMember.getUser().getEmail())
                .ProfileImage(groupMember.getUser().getProfileImage())
                .build();
    }

    //Dto와 StudyActivity 객체를 받아 출석체크 리스트를 저장하기
    private List<Attendance> createAttendanceListData(StudyActivity savedStudyActivity, StudyActivityDto dto){

        List<Attendance> attendances = dto.getStudyAttendanceDtoList().stream()
                .map(attendanceDto -> Attendance.builder()
                        .user(userRepository.findByStudentNumber(attendanceDto.getStudentNumber()).orElseThrow())
                        .studyGroup(savedStudyActivity.getStudyGroup())
                        .studyActivity(savedStudyActivity)
                        .type(attendanceDto.getType())
                        .week(savedStudyActivity.getWeek())
                        .attendanceDate(LocalDate.now())
                        .build())
                .collect(Collectors.toList());

        return attendanceRepository.saveAll(attendances);
    }

    // 스터디 활동 글 Response 반환
    private StudyActivityResponse createStudyActivityResponse(StudyActivity studyActivity, List<Attendance> attendanceList){

        List<StudyAttendanceDto> studyAttendanceDtoList = attendanceList.stream()
                .map(attendance -> StudyAttendanceDto.builder()
                        .studentNumber(attendance.getUser().getStudentNumber())
                        .name(attendance.getUser().getName())
                        .type(attendance.getType())
                        .build())
                .toList();

        return StudyActivityResponse.builder()
                .activityId(studyActivity.getActivityId())
                .title(studyActivity.getTitle())
                .content(studyActivity.getContent())
                .week(studyActivity.getWeek())
                .imageUrls(getImageUrlsFromEntity(studyActivity))
                .studyAttendanceDtoList(studyAttendanceDtoList)
                .createdAt(studyActivity.getCreatedAt())
                .updatedAt(studyActivity.getUpdatedAt())
                .build();
    }

    //SimpleStudyActivity 리스트로 반환
    private List<SimpleStudyActivityResponse> createSimpleStudyActivityList(List<StudyActivity> studyActivityList){

        return studyActivityList.stream()
                .map(activity -> SimpleStudyActivityResponse.builder()
                        .activityId(activity.getActivityId())
                        .title(activity.getTitle())
                        .week(activity.getWeek())
                        .imageUrls(getImageUrlsFromEntity(activity))
                        .createdAt(activity.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    //StudyMemberWarnResponse 반환
    private StudyMemberWarnResponse createStudyMemberWarnResponse(GroupMember groupMember){

        return StudyMemberWarnResponse.builder()
                .userId(groupMember.getUser().getUserId())
                .studentNumber(groupMember.getUser().getStudentNumber())
                .name(groupMember.getUser().getName())
                .email(groupMember.getUser().getEmail())
                .ProfileImage(groupMember.getUser().getProfileImage())
                .warn(groupMember.getWarn())
                .build();
    }

    public List<AllStudyGroupDto> getAllGroups(Long studentNumber) {

        User mentee = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        List<GroupMember> groupMembers = groupMemberRepository.findByUserWithStudyGroup(mentee);

        return groupMembers.stream()
                .map(GroupMember::getStudyGroup)
                .filter(Objects::nonNull)
                .map(studyGroup -> AllStudyGroupDto.builder()
                        .studyGroupId(studyGroup.getStudyId())
                        .name(studyGroup.getName())
                        .category(studyGroup.getCategory())
                        .studyImage(studyGroup.getStudyImage())
                        .generation(studyGroup.getGeneration())
                        .status(studyGroup.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    //그룹 상세 정보 DTO 생성
    private StudyGroupDetailDto createStudyGroupDetailDto(StudyGroup studyGroup, GroupMember mentor,
                                                          List<StudyGroupDetailDto.MenteeInfo> menteeList) {

        StudyGroupDetailDto.StudyGroupDetailDtoBuilder builder = StudyGroupDetailDto.builder()
                .studyId(studyGroup.getStudyId())
                .name(studyGroup.getName())
                .content(studyGroup.getContent())
                .category(studyGroup.getCategory())
                .generation(studyGroup.getGeneration())
                .studyImage(studyGroup.getStudyImage())
                .status(studyGroup.getStatus())
                .menteeList(menteeList)
                .menteeCount(menteeList.size())
                .createdAt(studyGroup.getCreatedAt())
                .updatedAt(studyGroup.getUpdatedAt());

        // 멘토 정보 설정
        if (mentor != null && mentor.getUser() != null) {
            builder.mentorName(mentor.getUser().getName())
                    .mentorStudentNumber(mentor.getUser().getStudentNumber());
        }

        return builder.build();
    }

    //멘티 정보 DTO 생성
    private StudyGroupDetailDto.MenteeInfo createMenteeInfo(GroupMember mentee) {

        return StudyGroupDetailDto.MenteeInfo.builder()
                .name(mentee.getUser().getName())
                .studentNumber(mentee.getUser().getStudentNumber())
                .email(mentee.getUser().getEmail())
                .build();
    }
}