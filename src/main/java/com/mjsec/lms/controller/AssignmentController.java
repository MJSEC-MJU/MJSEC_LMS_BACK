package com.mjsec.lms.controller;

import com.mjsec.lms.dto.*;
import com.mjsec.lms.service.AssignmentSubmissionService;
import com.mjsec.lms.service.PlanService;
import com.mjsec.lms.type.ResponseMessage;
import com.mjsec.lms.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
public class AssignmentController {

    private final PlanService planService;
    private final AssignmentSubmissionService assignmentSubmissionService;

    public AssignmentController(PlanService planService,
                                AssignmentSubmissionService assignmentSubmissionService) {

        this.planService = planService;
        this.assignmentSubmissionService = assignmentSubmissionService;
    }

    //전체 계획 조회하기
    @GetMapping("/{groupId}/plan")
    public ResponseEntity<SuccessResponse<List<PlanResponse>>> getPlan(
            @PathVariable Long groupId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<PlanResponse> ResponseList = planService.getPlan(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_SUCCESS,
                        ResponseList
                )
        );
    }

    //과제가 있는 계획만 조회하기
    @GetMapping("/{groupId}/assignment-plan")
    public ResponseEntity<SuccessResponse<List<PlanResponse>>> getOnlyAssignmentPlan(
            @PathVariable Long groupId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<PlanResponse> ResponseList = planService.getOnlyAssignmentPlan(groupId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_SUCCESS,
                        ResponseList
                )
        );
    }

    //계획 상세 조회하기
    @GetMapping("/{groupId}/plan/{planId}")
    public ResponseEntity<SuccessResponse<DetailPlanResponse>> getDetailedPlan(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailPlanResponse detailPlanResponse = planService.getDetailPlan(groupId, planId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_SUCCESS,
                        detailPlanResponse
                )
        );
    }

    //과제 제출하기 (멘티가)
    @PostMapping("/{groupId}/assignment/submit/{planId}")
    public ResponseEntity<SuccessResponse<SubmissionResponse>> submitAssignment(
            @Valid @RequestBody SubmissionDto dto,
            @PathVariable Long groupId,
            @PathVariable Long planId,
            Authentication authentication,
            HttpServletRequest request) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        //클라이언트 IP 뽑아내기
        String clientIpAddr = IpUtils.getClientIp(request);

        SubmissionResponse submissionResponse = assignmentSubmissionService.submitAssignment(groupId, planId, currentUserStudentNumber, dto, clientIpAddr);

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
    @GetMapping("/{groupId}/assignment/submit/{planId}")
    public ResponseEntity<SuccessResponse<List<SubmissionResponse>>> getSubmissionList(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            Authentication authentication
    ) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<SubmissionResponse> submissionResponseList = assignmentSubmissionService.getSubmissionList(groupId, planId, currentUserStudentNumber);

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
    @GetMapping("/{groupId}/assignment/submit/{planId}/submission/{submitId}")
    public ResponseEntity<SuccessResponse<DetailSubmissionResponse>> getUserDetailedSubmission(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long submitId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailSubmissionResponse detailSubmissionResponse = assignmentSubmissionService.getDetailedSubmission(groupId, planId, submitId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_CHECK_SUCCESS,
                        detailSubmissionResponse
                )
        );
    }

    //과제 제출 수정하기
    @PutMapping("/{groupId}/assignment/submit/{planId}/submission/{submitId}")
    public ResponseEntity<SuccessResponse<DetailSubmissionResponse>> updateSubmission(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long submitId,
            @RequestBody SubmissionDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailSubmissionResponse detailSubmissionResponse = assignmentSubmissionService.updateAssignmentSubmission(groupId, planId, submitId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_UPDATE_SUCCESS,
                        detailSubmissionResponse
                )
        );
    }

    //과제 제출 삭제하기
    @DeleteMapping("/{groupId}/assignment/submit/{planId}/submission/{submitId}")
    public ResponseEntity<SuccessResponse<Void>> deleteSubmission(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long submitId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentSubmissionService.deleteAssignmentSubmission(groupId, planId, submitId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_DELETE_SUCCESS
                )
        );
    }

    //댓글 생성하기
    @PostMapping("/{groupId}/plan/{planId}/create-comment")
    public ResponseEntity<SuccessResponse<PlanCommentResponse>> createPlanComment(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @RequestBody PlanCommentDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        PlanCommentResponse planCommentResponse = planService.createPlanComment(groupId, planId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.COMMENT_CREATE_SUCCESS,
                        planCommentResponse
                )
        );
    }

    //댓글 수정하기
    @PutMapping("/{groupId}/plan/{planId}/comment/{commentId}")
    public ResponseEntity<SuccessResponse<PlanCommentResponse>> updatePlanComment(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long commentId,
            @RequestBody PlanCommentDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        PlanCommentResponse planCommentResponse = planService.updatePlanComment(groupId, planId, commentId, currentUserStudentNumber,dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.COMMENT_UPDATE_SUCCESS,
                        planCommentResponse
                )
        );
    }

    //계획 댓글 삭제하기
    @DeleteMapping("/{groupId}/plan/{planId}/comment/{commentId}")
    public ResponseEntity<SuccessResponse<Void>> deletePlanComment(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long commentId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        planService.deletePlanComment(groupId,planId,commentId,currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.COMMENT_DELETE_SUCCESS
                )
        );
    }


    //과제 제출 피드백 남기기
    @PostMapping("/{groupId}/assignment/submit/{planId}/submission/{submitId}/feedback")
    public ResponseEntity<SuccessResponse<Void>> leaveFeedback(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long submitId,
            @RequestBody SubmissionFeedbackDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentSubmissionService.leaveFeedback(groupId, planId, submitId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.FEEDBACK_LEAVE_SUCCESS
                )
        );
    }


    //과제 피드백 수정하기
    @PutMapping("/{groupId}/assignment/submit/{planId}/submission/{submitId}/feedback")
    public ResponseEntity<SuccessResponse<SubmissionFeedbackDto>> updateFeedback(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long submitId,
            @RequestBody SubmissionFeedbackDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        SubmissionFeedbackDto submissionFeedbackDto = assignmentSubmissionService.updateFeedback(groupId, planId, submitId, currentUserStudentNumber, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.FEEDBACK_UPDATE_SUCCESS
                )
        );
    }

    //과제 피드백 삭제하기
    @DeleteMapping("/{groupId}/assignment-submit/{planId}/submission/{submitId}/feedback")
    public ResponseEntity<SuccessResponse<Void>> deleteFeedback(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @PathVariable Long submitId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentSubmissionService.deleteFeedback(groupId, planId, submitId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.FEEDBACK_DELETE_SUCCESS
                )
        );
    }
}