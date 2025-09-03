package com.mjsec.lms.service;

import com.mjsec.lms.domain.*;
import com.mjsec.lms.dto.*;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanCommentRepository planCommentRepository;
    private final ValidationUtils validationUtils;

    public PlanService(PlanRepository planRepository,
                       PlanCommentRepository planCommentRepository,
                       ValidationUtils validationUtils) {

        this.planRepository = planRepository;
        this.planCommentRepository = planCommentRepository;
        this.validationUtils = validationUtils;
    }

    // 계획 등록하기 (멘토만 가능함.)
    @Transactional
    public DetailPlanResponse createPlan(Long groupId, PlanDto dto, Long currentUserStudentNumber) {

        log.info("createAssignment called with groupId: {}, dto: {}, currentUserStudentNumber: {}", groupId, dto, currentUserStudentNumber);

        User user = validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        log.info("User {} is confirmed as MENTOR of StudyGroup: {}", user.getUserId(), studyGroup.getName());

        Plan plan = Plan.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .hasAssignment(dto.isHasAssignment())
                .creator(user)
                .studyGroup(studyGroup)
                .createdAt(LocalDateTime.now())
                .build();

        planRepository.save(plan);
        log.info("Assignment created successfully: {}", plan);

        return createDetailPlanResponse(plan);
    }

    // 전체 계획 조회하기
    public List<PlanResponse> getPlan(Long groupId, Long currentUserStudentNumber) {

        log.info("getPlan called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);

        List<Plan> planList = planRepository.findAllByStudyGroup_StudyId(groupId);
        log.info("Found {} plans in StudyGroup", planList.size());

        List<PlanResponse> planResponses = planList.stream()
                .map(this::createResponse)
                .collect(Collectors.toList());

        log.info("get Assignment Successfully!");
        return planResponses;
    }

    // 계획 상세 조회하기
    public DetailPlanResponse getDetailPlan(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("getDetailPlan called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        Plan plan = validationUtils.validatePlan(assignmentId);

        log.info("Found plan: {}", plan);
        return createDetailPlanResponse(plan);
    }

    // 계획 수정하기 (멘토만 가능함.)
    @Transactional
    public DetailPlanResponse updatePlan(Long groupId, Long assignmentId, PlanDto dto, Long currentUserStudentNumber) {

        log.info("updateAssignment called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        Plan plan = validationUtils.validatePlan(assignmentId);

        log.info("Found assignment: {}", plan);

        updatePlanData(plan, dto);
        return createDetailPlanResponse(plan);
    }

    // 계획 삭제하기 (멘토만 가능함.)
    @Transactional
    public void deletePlan(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("deletePlan called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        Plan plan = validationUtils.validatePlan(assignmentId);

        log.info("Found assignment: {}", plan);

        planRepository.delete(plan);
        log.info("Assignment deleted successfully: {}", plan);
    }

    // 계획 댓글 생성
    @Transactional
    public PlanCommentResponse createPlanComment(Long groupId, Long assignmentId, Long currentUserStudentNumber, PlanCommentDto dto) {

        log.info("createAssignmentComment called");

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        Plan plan = validationUtils.validatePlan(assignmentId);
        validationUtils.validateComment(dto.getContent());

        PlanComment planComment = PlanComment.builder()
                .plan(plan)
                .author(user)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        planCommentRepository.save(planComment);

        return createPlanCommentResponse(planComment);
    }

    /**
     * === 데이터 처리 메서드들 ===
     */

    // Plan 데이터를 업데이트
    private void updatePlanData(Plan plan, PlanDto dto) {

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            plan.setTitle(dto.getTitle());
        }

        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            plan.setContent(dto.getContent());
        }

        if (dto.getStartDate() != null) {
            plan.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            plan.setEndDate(dto.getEndDate());
        }

        plan.setHasAssignment(dto.isHasAssignment());

        planRepository.save(plan);
        log.info("Assignment updated successfully: {}", plan);
    }

    /**
     * === DTO 변환 메서드들 ===
     */

    // Plan를 DetailPlanResponse로 변환
    private DetailPlanResponse createDetailPlanResponse(Plan plan) {

        List<PlanComment> assignmentCommentList = planCommentRepository.findAllByAssignmentAssignId(plan.getPlanId());
        List<PlanCommentResponse> commentList = assignmentCommentList.stream()
                .map(this::createPlanCommentResponse)
                .collect(Collectors.toList());

        return DetailPlanResponse.builder()
                .planId(plan.getPlanId())
                .title(plan.getTitle())
                .content(plan.getContent())
                .hasAssignment(plan.isHasAssignment())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .creatorName(plan.getCreator().getName())
                .createdAt(plan.getCreatedAt())
                .commentCount(assignmentCommentList.size())
                .commentList(commentList)
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    // Plan를 PlanResponse로 변환
    private PlanResponse createResponse(Plan plan) {

        return PlanResponse.builder()
                .planId(plan.getPlanId())
                .title(plan.getTitle())
                .content(plan.getContent())
                .hasAssignment(plan.isHasAssignment())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .createdAt(plan.getCreatedAt())
                .build();
    }


    // 계획 댓글 Response 생성해서 반환하기
    private PlanCommentResponse createPlanCommentResponse(PlanComment assignmentComment) {

        return PlanCommentResponse.builder()
                .commentId(assignmentComment.getCommentId())
                .content(assignmentComment.getContent())
                .planId(assignmentComment.getPlan().getPlanId())
                .creatorName(assignmentComment.getAuthor().getName())
                .createdAt(assignmentComment.getCreatedAt())
                .build();
    }
}