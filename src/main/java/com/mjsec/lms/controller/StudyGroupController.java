package com.mjsec.lms.controller;

import com.mjsec.lms.dto.*;
import com.mjsec.lms.service.StudyGroupService;
import com.mjsec.lms.type.ResponseMessage;
import com.mjsec.lms.util.ValidationUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
@Slf4j
public class StudyGroupController {

    private final StudyGroupService studyGroupService;
    private final ValidationUtils validationUtils;

    StudyGroupController(StudyGroupService studyGroupService, ValidationUtils validationUtils) {

        this.studyGroupService = studyGroupService;
        this.validationUtils = validationUtils;
    }

    //스터디 멤버 전체 반환
    @GetMapping("/{groupId}/member")
    public ResponseEntity<SuccessResponse<List<StudyMemberResponse>>> getStudyGroupMemberList(
            @PathVariable Long groupId,
            Authentication authentication
    ){
        log.info("Getting study group member list for group: {}", groupId);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<StudyMemberResponse> studyMemberResponseList = studyGroupService.getStudyMemberList(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_MEMBER_GET_SUCCESS,
                        studyMemberResponseList
                )
        );
    }


    //스터디 멘티 멤버만 반환
    @GetMapping("/{groupId}/mentee")
    public ResponseEntity<SuccessResponse<List<StudyMemberResponse>>> getStudyMenteeList(
            @PathVariable Long groupId,
            Authentication authentication
    ){
        log.info("Getting study group mentee list for group: {}", groupId);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<StudyMemberResponse> studyMenteeResponseList = studyGroupService.getStudyMenteeList(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_MENTEE_GET_SUCCESS,
                        studyMenteeResponseList
                )
        );
    }

    //스터디 멘티들 경고 횟수 조회하기
    @GetMapping("/{groupId}/mentee/warn")
    public ResponseEntity<SuccessResponse<List<StudyMemberWarnResponse>>> getStudyMenteeWarnList(
            @PathVariable Long groupId,
            Authentication authentication
    ){

        log.info("Getting study group mentee warn list for group: {}", groupId);

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<StudyMemberWarnResponse> studyMemberWarnResponseList = studyGroupService.getStudyMemberWarnList(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.GET_ALL_WARN_SUCCESS,
                        studyMemberWarnResponseList
                )
        );
    }

    //활동 글 생성
    @PostMapping("/{groupId}/create-activity")
    public ResponseEntity<SuccessResponse<StudyActivityResponse>> createStudyActivity(
            @PathVariable Long groupId,
            @Valid @RequestPart("studyActivityDto") StudyActivityDto studyActivityDto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication
    ){
        log.info("Creating study activity for group: {}", groupId);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        validationUtils.validateActivityContent(studyActivityDto.getTitle(), studyActivityDto.getContent());

        StudyActivityResponse studyActivityResponse = studyGroupService.createStudyActivity(groupId, currentUserStudentNumber, studyActivityDto, image);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_SUCCESS,
                        studyActivityResponse
                )
        );
    }

    //활동 글 수정하기
    @PutMapping("/{groupId}/activity/{activityId}")
    public ResponseEntity<SuccessResponse<StudyActivityResponse>> updateStudyActivity(
            @PathVariable Long groupId,
            @PathVariable Long activityId,
            @Valid @RequestPart("studyActivityDto") StudyActivityDto studyActivityDto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        log.info("Updating study activity: {} for group: {}", activityId, groupId);

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        validationUtils.validateActivityBelongsToGroup(activityId, groupId); // 연관관계 검증
        validationUtils.validateActivityContent(studyActivityDto.getTitle(), studyActivityDto.getContent()); // 내용 검증

        StudyActivityResponse studyActivityResponse = studyGroupService.updateStudyActivity(
                groupId, activityId, currentUserStudentNumber, studyActivityDto, image);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_UPDATE_SUCCESS,
                        studyActivityResponse
                )
        );
    }

    //활동 글 모두 조회
    @GetMapping("/{groupId}/activity-list")
    public ResponseEntity<SuccessResponse<List<SimpleStudyActivityResponse>>> getStudyActivityList(
            @PathVariable Long groupId,
            Authentication authentication){

        log.info("Getting study activity list for group: {}", groupId);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<SimpleStudyActivityResponse> studyActivityRsponseList = studyGroupService.getStudyActivityList(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_GET_SUCCESS,
                        studyActivityRsponseList
                )
        );
    }

    //활동 글 상세 조회
    @GetMapping("/{groupId}/activity/{activityId}")
    public ResponseEntity<SuccessResponse<StudyActivityResponse>> getStudyActivity(
            @PathVariable Long groupId,
            @PathVariable Long activityId,
            Authentication authentication
    ){
        log.info("Getting study activity: {} for group: {}", activityId, groupId);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        // 연관관계 검증 추가
        validationUtils.validateActivityBelongsToGroup(activityId, groupId);

        StudyActivityResponse studyActivityResponse = studyGroupService.getStudyActivity(groupId, activityId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_GET_SUCCESS,
                        studyActivityResponse
                )
        );
    }

    //활동 글 삭제
    @DeleteMapping("/{groupId}/activity/{activityId}")
    public ResponseEntity<SuccessResponse<Void>> deleteStudyActivity(
            @PathVariable Long groupId,
            @PathVariable Long activityId,
            Authentication authentication){

        log.info("Deleting study activity: {} from group: {}", activityId, groupId);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        //연관관계 검증 추가
        validationUtils.validateActivityBelongsToGroup(activityId, groupId);

        studyGroupService.deleteStudyActivity(groupId, activityId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_DELETE_SUCCESS
                )
        );
    }

    @GetMapping("/all")
    public ResponseEntity<SuccessResponse<List<AllStudyGroupDto>>> getAllGroups(Authentication authentication) {

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        List<AllStudyGroupDto> groups = studyGroupService.getAllGroups(currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.GET_ALL_GROUPS_SUCCESS,
                        groups
                )
        );
    }

    // 특정 그룹 상세 정보 조회
    @GetMapping("/{groupId}/info")
    public ResponseEntity<SuccessResponse<StudyGroupDetailDto>> getStudyGroupDetail(
            @PathVariable Long groupId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        StudyGroupDetailDto studyGroupDetail = studyGroupService.getStudyGroupDetail(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_GROUP_DETAIL_GET_SUCCESS,
                        studyGroupDetail
                )
        );
    }
}