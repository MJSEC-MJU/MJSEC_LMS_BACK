package com.mjsec.lms.studygroup.controller;

import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.common.dto.SuccessResponse;
import com.mjsec.lms.studygroup.dto.StudyGroupPutResponse;
import com.mjsec.lms.studygroup.dto.StudyGroupPutDto;
import com.mjsec.lms.assignment.dto.DetailPlanResponse;
import com.mjsec.lms.assignment.dto.PlanDto;
import com.mjsec.lms.studygroup.service.MentorService;
import com.mjsec.lms.assignment.service.PlanService;
import com.mjsec.lms.studygroup.service.StudyGroupService;
import com.mjsec.lms.common.type.ResponseMessage;
import com.mjsec.lms.common.util.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mentor")
public class MentorController {

    private final ValidationUtils validationUtils;
    private final MentorService mentorService;
    private final PlanService planService;
    private final StudyGroupService studyGroupService;

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

    //스터디 그룹 수정하기
    @PutMapping("/group/{groupId}")
    public ResponseEntity<SuccessResponse<StudyGroupPutResponse>> updateStudyGroup(
            @PathVariable("groupId") Long groupId,
            @Valid @RequestPart(value = "StudyGroupPutDto", required = false) StudyGroupPutDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication){

        validationUtils.validateStudyGroup(groupId);

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        StudyGroupPutResponse response = mentorService.updateStudyGroup(groupId,currentUserStudentNumber,image, dto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.UPDATE_GROUP_SUCCESS,
                        response
                )
        );
    }

    // 계획 등록하기
    @PostMapping("/group/{groupId}/create-plan")
    public ResponseEntity<SuccessResponse<DetailPlanResponse>> createPlan(
            @PathVariable Long groupId,
            @Valid @RequestBody PlanDto dto,
            Authentication authentication) {

        validationUtils.validatePlanDates(dto.getStartDate(), dto.getEndDate());

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

        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        if (dto.getStartDate() != null || dto.getEndDate() != null) {
            // 날짜 수정 시에만 검증
            Plan existingPlan = validationUtils.validatePlan(planId);
            LocalDateTime newStart = dto.getStartDate() != null ? dto.getStartDate() : existingPlan.getStartDate();
            LocalDateTime newEnd = dto.getEndDate() != null ? dto.getEndDate() : existingPlan.getEndDate();
            validationUtils.validatePlanDates(newStart, newEnd);
        }

        Long currentUserStudentNumber = (Long) authentication.getPrincipal();
        DetailPlanResponse detailPlanResponse = planService.updatePlan(groupId, planId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.PLAN_UPDATE_SUCCESS,
                        detailPlanResponse
                )
        );
    }

    // 등록한 계획 삭제하기
    @DeleteMapping("/group/{groupId}/plan/{planId}")
    public ResponseEntity<SuccessResponse<Void>> deletePlan(
            @PathVariable Long groupId,
            @PathVariable Long planId,
            Authentication authentication) {

        validationUtils.validatePlanBelongsToGroup(planId, groupId);

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