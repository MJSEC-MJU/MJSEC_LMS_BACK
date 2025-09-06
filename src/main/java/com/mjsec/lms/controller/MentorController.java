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
            Authentication authentication) {

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
            Authentication authentication) {

        Long currentStudentNumber = (Long) authentication.getPrincipal();
        mentorService.deleteMember(currentStudentNumber, groupId, studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.DELETE_MEMBER_SUCCESS
                )
        );
    }

    // 계획 등록하기
    @PostMapping("/group/{groupId}/create-plan")
    public ResponseEntity<SuccessResponse<DetailPlanResponse>> createPlan(
            @PathVariable Long groupId,
            @Valid @RequestBody PlanDto dto,
            Authentication authentication) {

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        DetailPlanResponse response = planService.createPlan(groupId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_CREATE_SUCCESS,
                        response
                )
        );
    }

    // 등록한 계획 수정하기
    @PutMapping("/group/{groupId}/plan/{planId}")
    public ResponseEntity<SuccessResponse<DetailPlanResponse>> updatePlan(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            @RequestBody PlanDto dto,
            Authentication authentication) {

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        DetailPlanResponse detailPlanResponse = planService.updatePlan(groupId, planId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_UPDATE_SUCCESS,
                        detailPlanResponse
                )
        );
    }

    // 등록한 과제 삭제하기
    @DeleteMapping("/group/{groupId}/plan/{planId}")
    public ResponseEntity<SuccessResponse<Void>> deletePlan(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            Authentication authentication) {

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        planService.deletePlan(groupId, planId, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_DELETE_SUCCESS
                )
        );
    }

    @PostMapping("/warn/{groupId}/{studentNumber}")
    public ResponseEntity<SuccessResponse<Void>> warnMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("studentNumber") Long studentNumber,
            Authentication authentication) {

        Long currentStudentNumber = (Long) authentication.getPrincipal();
        mentorService.warnMember(currentStudentNumber, groupId, studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.WARN_MEMBER_SUCCESS
                )
        );
    }
}