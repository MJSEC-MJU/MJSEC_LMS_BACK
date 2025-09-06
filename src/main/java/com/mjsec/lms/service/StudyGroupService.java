package com.mjsec.lms.service;

import com.mjsec.lms.domain.*;
import com.mjsec.lms.dto.*;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

    StudyGroupService(StudyGroupRepository studyGroupRepository,
                      ValidationUtils validationUtils,
                      StudyActivityRepository studyActivityRepository,
                      UserRepository userRepository,
                      AttendanceRepository attendanceRepository,
                      GroupMemberRepository groupMemberRepository,
                      FileService fileService){

        this.studyGroupRepository = studyGroupRepository;
        this.validationUtils = validationUtils;
        this.studyActivityRepository = studyActivityRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.fileService = fileService;
    }

    //스터디 그룹 멤버 전체 반환
    @Transactional(readOnly = true)
    public List<StudyMemberResponse> getStudyMemberList(Long groupId, Long currentUserStudentNumber){

        log.info("getStudyMemberList called");

        validationUtils.validateUser(currentUserStudentNumber);
        validationUtils.validateStudyGroup(groupId);

        List<GroupMember> groupMemberList = groupMemberRepository.findByStudyGroup_StudyId(groupId);

        return groupMemberList.stream()
                .map(this::createStudyMemberResponse)
                .collect(Collectors.toList());
    }

    //스터디 활동 글 + 출석체크 설정하기
    @Transactional
    public StudyActivityResponse createStudyActivity(Long groupId, Long currentUserStudentNumber,
                                                     StudyActivityDto studyActivityDto, MultipartFile image) {

        log.info("createStudyActivity called!");

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);

        // 이미지가 있으면 업로드
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileService.uploadImage(image);
            studyActivityDto.setImageUrl(imageUrl);
        }

        StudyActivity savedStudyActivity = createStudyActivityData(groupId, studyActivityDto);
        List<Attendance> attendanceList = createAttendanceListData(savedStudyActivity, studyActivityDto);

        return createStudyActivityResponse(savedStudyActivity, attendanceList);
    }

    @Transactional
    public StudyActivityResponse updateStudyActivity(Long groupId, Long activityId,
                                                     Long currentUserStudentNumber, StudyActivityDto studyActivityDto, MultipartFile image) {

        log.info("updateStudyActivity called");

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);
        StudyActivity studyActivity = validationUtils.validateStudyActivity(activityId);

        // 기존 이미지 삭제 (새 이미지가 있는 경우)
        if (image != null && !image.isEmpty()) {
            if (studyActivity.getImageUrl() != null) {
                fileService.deleteImage(studyActivity.getImageUrl());
            }
            String newImageUrl = fileService.uploadImage(image);
            studyActivity.setImageUrl(newImageUrl);
        }

        // 다른 필드들 업데이트
        updateStudyActivityData(studyActivity, studyActivityDto);

        return createStudyActivityResponse(studyActivity, studyActivity.getAttendances());
    }

    // 스터디 활동 글 전체 조회
    public List<SimpleStudyActivityResponse> getStudyActivityList(Long groupId, Long currentUserStudentNumber){

        log.info("getStudyActivityList called");

        //검증 로직 (사용자, 스터디그룹)
        validationUtils.validateBasicAccess(groupId,currentUserStudentNumber);

        List<StudyActivity> studyActivityList = studyActivityRepository.findAllByStudyGroupId(groupId);

        return createSimpleStudyActivityList(studyActivityList);
    }

    // 스터디 활동 글 삭제
    public void deleteStudyActivity(Long groupId, Long activityId, Long currentUserStudentNumber){

        log.info("deleteStudyActivity called");

        User user = validationUtils.validateBasicAccess(groupId,currentUserStudentNumber);
        validationUtils.validateMentorRole(user.getUserId(), groupId);
        StudyActivity studyActivity = validationUtils.validateStudyActivity(activityId);

        studyActivityRepository.delete(studyActivity);

        log.info("StudyActivity({}) deleted", activityId);
    }

    //활동 글 상세 조회
    @Transactional(readOnly = true)
    public StudyActivityResponse getStudyActivity(Long groupId, Long activityId, Long currentUserStudentNumber){

        log.info("getStudyActivity called");

        User user = validationUtils.validateBasicAccess(groupId,currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);
        validationUtils.validateGroupMembership(user,studyGroup);
        StudyActivity studyActivity = validationUtils.validateStudyActivity(activityId);

        return createStudyActivityResponse(studyActivity, studyActivity.getAttendances());
    }

    /*
    DTO 생성용 메서드
     */

    //Dto를 받아서 StudyActivity 저장하기
    public StudyActivity createStudyActivityData(Long groupId, StudyActivityDto studyActivityDto){

        StudyActivity studyActivity = StudyActivity.builder()
                .title(studyActivityDto.getTitle())
                .content(studyActivityDto.getContent())
                .studyGroup(studyGroupRepository.findById(groupId).orElseThrow())
                .week(studyActivityDto.getWeek())
                .imageUrl(studyActivityDto.getImageUrl())
                .build();

        return studyActivityRepository.save(studyActivity);
    }

    private void updateStudyActivityData(StudyActivity studyActivity, StudyActivityDto dto) {
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            studyActivity.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            studyActivity.setContent(dto.getContent());
        }
        if (dto.getWeek() != null && !dto.getWeek().trim().isEmpty()) {
            studyActivity.setWeek(dto.getWeek());
        }

        studyActivityRepository.save(studyActivity);
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
    public List<Attendance> createAttendanceListData(StudyActivity savedStudyActivity, StudyActivityDto dto){

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
    public StudyActivityResponse createStudyActivityResponse(StudyActivity studyActivity, List<Attendance> attendanceList){

        List<StudyAttendanceDto> studyAttendanceDtoList = attendanceList.stream()
                .map(attendance -> StudyAttendanceDto.builder()
                        .studentNumber(attendance.getUser().getStudentNumber())
                        .name(attendance.getUser().getName())
                        .type(attendance.getType())
                        .build())
                .toList();

        return StudyActivityResponse.builder()
                .StudyActivityId(studyActivity.getActivityId())
                .title(studyActivity.getTitle())
                .content(studyActivity.getContent())
                .week(studyActivity.getWeek())
                .imageUrl(studyActivity.getImageUrl())
                .studyAttendanceDtoList(studyAttendanceDtoList)
                .createdAt(studyActivity.getCreatedAt())
                .build();
    }

    //SimpleStudyActivity 리스트로 반환
    public List<SimpleStudyActivityResponse> createSimpleStudyActivityList(List<StudyActivity> studyActivityList){

        return studyActivityList.stream()
                .map(activity -> SimpleStudyActivityResponse.builder()
                        .activityId(activity.getActivityId())
                        .title(activity.getTitle())
                        .week(activity.getWeek())
                        .imageUrl(activity.getImageUrl())
                        .createdAt(activity.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }


}
