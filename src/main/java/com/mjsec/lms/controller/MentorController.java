package com.mjsec.lms.controller;

import com.mjsec.lms.dto.DetailPlanResponse;
import com.mjsec.lms.dto.PlanDto;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.MentorService;
import com.mjsec.lms.service.PlanService;
import com.mjsec.lms.type.ResponseMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mentor")
public class MentorController {

    private final MentorService mentorService;
    private final PlanService planService;

    @PostMapping("/group/{groupId}/add-member/{studentNumber}")
    public ResponseEntity<SuccessResponse<Void>> addMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("studentNumber") Long studentNumber,
            Authentication authentication)
    {

        Long currentStudentNumber = (Long) authentication.getPrincipal();
        mentorService.addMember(currentStudentNumber, groupId, studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.ADD_MEMBER_SUCCESS
                )
        );
    }

    @DeleteMapping("/group/{groupId}/delete-member/{studentNumber}")
    public ResponseEntity<SuccessResponse<Void>> deleteMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("studentNumber") Long studentNumber,
            Authentication authentication)
    {
        Long currentStudentNumber = (Long) authentication.getPrincipal();
        mentorService.deleteMember(currentStudentNumber, groupId, studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.DELETE_MEMBER_SUCCESS
                )
        );
    }

    //계획 등록하기
    @PostMapping("/group/{groupId}/create-plan")
    public ResponseEntity<SuccessResponse<DetailPlanResponse>> createPlan(
            @PathVariable Long groupId,
            @Valid @RequestBody PlanDto dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailPlanResponse response = planService.createPlan(groupId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_CREATE_SUCCESS,
                        response
                )
        );
    }

    //등록한 계획 수정하기
    @PutMapping("/group/{groupId}/plan/{planId}")
    public ResponseEntity<SuccessResponse<DetailPlanResponse>> updatePlan(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @RequestBody PlanDto dto,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailPlanResponse detailAssignmentResponse = planService.updatePlan(groupId, assignId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_UPDATE_SUCCESS,
                        detailAssignmentResponse
                )
        );
    }

    //등록한 과제 삭제하기
    @DeleteMapping("/group/{groupId}/plan/{planId}")
    public ResponseEntity<SuccessResponse<Void>> deletePlan(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication) {

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        planService.deletePlan(groupId, assignId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_DELETE_SUCCESS
                )
        );
    }
}
