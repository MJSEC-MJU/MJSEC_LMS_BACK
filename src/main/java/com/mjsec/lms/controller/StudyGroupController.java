package com.mjsec.lms.controller;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.dto.*;
import com.mjsec.lms.service.StudyGroupService;
import com.mjsec.lms.type.ResponseMessage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    StudyGroupController(StudyGroupService studyGroupService){

        this.studyGroupService = studyGroupService;
    }

    //스터디 멤버 전체 반환
    @GetMapping("/{groupId}/member")
    public ResponseEntity<SuccessResponse<List<StudyMemberResponse>>> getStudyGroupMemberList(
            @PathVariable Long groupId,
            Authentication authentication
    ){

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

    //활동 글 생성
    @PostMapping("/{groupId}/create-activity")
    public ResponseEntity<SuccessResponse<StudyActivityResponse>> createStudyActivity(
            @PathVariable Long groupId,
            @Valid @RequestPart("studyActivityDto") StudyActivityDto studyActivityDto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication
    ){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

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

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

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

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

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

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        studyGroupService.deleteStudyActivity(groupId, activityId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_DELETE_SUCCESS
                )
        );
    }
}
