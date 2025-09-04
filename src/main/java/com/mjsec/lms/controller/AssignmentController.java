package com.mjsec.lms.controller;

import com.mjsec.lms.dto.*;
import com.mjsec.lms.service.AssignmentService;
import com.mjsec.lms.type.ResponseMessage;
import com.mjsec.lms.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {

        this.assignmentService = assignmentService;
    }

    //과제 등록하기
    @PostMapping("/{groupId}/create-assignment")
    public ResponseEntity<SuccessResponse<DetailAssignmentResponse>> createAssignment(
            @PathVariable Long groupId,
            @Valid @RequestBody AssignmentDto dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailAssignmentResponse response = assignmentService.createAssignment(groupId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_CREATE_SUCCESS,
                        response
                )
        );
    }

    //전체 과제 조회하기
    @GetMapping("/{groupId}/assignments")
    public ResponseEntity<SuccessResponse<List<AssignmentResponse>>> getAssignments(
            @PathVariable Long groupId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<AssignmentResponse> assignmentResponseList = assignmentService.getAssignment(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUCCESS,
                        assignmentResponseList
                )
        );
    }

    //과제 상세 조회하기
    @GetMapping("/{groupId}/assignments/{assignId}")
    public ResponseEntity<SuccessResponse<DetailAssignmentResponse>> getDetailedAssignment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailAssignmentResponse detailAssignmentResponse = assignmentService.getDetailAssignment(groupId, assignId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUCCESS,
                        detailAssignmentResponse
                )
        );
    }

    //등록한 과제 수정하기
    @PutMapping("/{groupId}/assignments/{assignId}")
    public ResponseEntity<SuccessResponse<DetailAssignmentResponse>> updateAssignment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @RequestBody AssignmentDto dto,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailAssignmentResponse detailAssignmentResponse = assignmentService.updateAssignment(groupId, assignId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_UPDATE_SUCCESS,
                        detailAssignmentResponse
                )
        );
    }

    //등록한 과제 삭제하기
    @DeleteMapping("/{groupId}/assignments/{assignId}")
    public ResponseEntity<SuccessResponse<Void>> deleteAssignment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentService.deleteAssignment(groupId, assignId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_DELETE_SUCCESS
                )
        );
    }

    //과제 제출하기 (멘티가)
    @PostMapping("/{groupId}/assign-submit/{assignId}")
    public ResponseEntity<SuccessResponse<SubmissionResponse>> submitAssignment(
            @Valid @RequestBody SubmissionDto dto,
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication,
            HttpServletRequest request) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        //클라이언트 IP 뽑아내기
        String clientIpAddr = IpUtils.getClientIp(request);

        SubmissionResponse submissionResponse = assignmentService.submitAssignment(groupId, assignId, currentUserStudentNumber, dto, clientIpAddr);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_SUCCESS,
                        submissionResponse
                )
        );
    }

    /**
     * 전체 과제 제출 리스트 확인 가능
     */
    @GetMapping("/{groupId}/assign-submit/{assignId}/submissions")
    public ResponseEntity<SuccessResponse<List<SubmissionResponse>>> getSubmissionList(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication
    ) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<SubmissionResponse> submissionResponseList = assignmentService.getSubmissionList(groupId, assignId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_CHECK_SUCCESS,
                        submissionResponseList
                )
        );
    }

    /**
    * 유저별 과제 제출 확인하기 (멘토/멘티 둘 다 가능)
     * 멘티 (자기 자신 과제만 조회 가능)
     * 멘토 (나머지도 다 가능)
     */
    @GetMapping("/{groupId}/assign-submit/{assignId}/submissions/{submitId}")
    public ResponseEntity<SuccessResponse<DetailSubmissionResponse>> getUserDetailedSubmission(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @PathVariable Long submitId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailSubmissionResponse detailSubmissionResponse = assignmentService.getDetailedSubmission(groupId, assignId, submitId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_CHECK_SUCCESS,
                        detailSubmissionResponse
                )
        );
    }

    //과제 제출 수정하기
    @PutMapping("/{groupId}/assign-submit/{assignId}/submissions/{submitId}")
    public ResponseEntity<SuccessResponse<DetailSubmissionResponse>> updateSubmission(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @PathVariable Long submitId,
            @RequestBody SubmissionDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailSubmissionResponse detailSubmissionResponse = assignmentService.updateAssignmentSubmission(groupId, assignId, submitId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_UPDATE_SUCCESS,
                        detailSubmissionResponse
                )
        );
    }

    //과제 제출 삭제하기
    @DeleteMapping("/{groupId}/assign-submit/{assignId}/submissions/{submitId}")
    public ResponseEntity<SuccessResponse<Void>> deleteSubmission(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @PathVariable Long submitId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentService.deleteAssignmentSubmission(groupId, assignId, submitId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_DELETE_SUCCESS
                )
        );
    }

    //댓글 생성하기
    @PostMapping("/{groupId}/assignments/{assignId}/create-comment")
    public ResponseEntity<SuccessResponse<AssignmentCommentResponse>> createAssignmentComment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @RequestBody AssignmentCommentDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        AssignmentCommentResponse assignmentComment = assignmentService.createAssignmentComment(groupId, assignId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.COMMENT_CREATE_SUCCESS,
                        assignmentComment
                )
        );
    }

    //과제 제출 피드백 남기기
    @PostMapping("/{groupId}/assign-submit/{assignId}/submissions/{submitId}/feedback")
    public ResponseEntity<SuccessResponse<Void>> leaveFeedback(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @PathVariable Long submitId,
            @RequestBody SubmissionFeedbackDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentService.leaveFeedback(groupId, assignId, submitId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.FEEDBACK_LEAVE_SUCCESS
                )
        );
    }


    //과제 피드백 수정하기
    @PutMapping("/{groupId}/assign-submit/{assignId}/submissions/{submitId}/feedback")
    public ResponseEntity<SuccessResponse<SubmissionFeedbackDto>> updateFeedback(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @PathVariable Long submitId,
            @RequestBody SubmissionFeedbackDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        SubmissionFeedbackDto submissionFeedbackDto = assignmentService.updateFeedback(groupId, assignId, submitId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.FEEDBACK_UPDATE_SUCCESS
                )
        );
    }

    //과제 피드백 삭제하기
    @DeleteMapping("/{groupId}/assign-submit/{assignId}/submissions/{submitId}/feedback")
    public ResponseEntity<SuccessResponse<Void>> deleteFeedback(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @PathVariable Long submitId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentService.deleteFeedback(groupId, assignId, submitId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.FEEDBACK_DELETE_SUCCESS
                )
        );
    }
}