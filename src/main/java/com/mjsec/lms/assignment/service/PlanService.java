package com.mjsec.lms.assignment.service;

import com.mjsec.lms.user.domain.User;
import com.mjsec.lms.studygroup.domain.StudyGroup;
import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.assignment.domain.PlanComment;
import com.mjsec.lms.assignment.dto.PlanDto;
import com.mjsec.lms.assignment.dto.DetailPlanResponse;
import com.mjsec.lms.assignment.dto.PlanResponse;
import com.mjsec.lms.assignment.dto.PlanCommentDto;
import com.mjsec.lms.assignment.dto.PlanCommentResponse;
import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.assignment.repository.PlanCommentRepository;
import com.mjsec.lms.common.util.ValidationUtils;
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
        log.info("createPlan called with groupId: {}, dto: {}, currentUserStudentNumber: {}",
                groupId, dto, currentUserStudentNumber);

        //검증 로직
        User user = validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        // 계획 날짜 검증
        validationUtils.validatePlanDates(dto.getStartDate(), dto.getEndDate());

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
        log.info("Plan created successfully with ID: {}", plan.getPlanId());

        return createDetailPlanResponse(plan);
    }

    // 전체 계획 조회하기
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlan(Long groupId, Long currentUserStudentNumber) {
        log.info("getPlan called for group: {} by user: {}", groupId, currentUserStudentNumber);

        //검증 로직
        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);

        List<Plan> planList = planRepository.findAllByStudyGroup_StudyId(groupId);
        log.info("Found {} plans in StudyGroup: {}", planList.size(), groupId);

        List<PlanResponse> planResponses = planList.stream()
                .map(this::createResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} plans", planResponses.size());
        return planResponses;
    }

    // 과제가 있는 계획만 조회하기
    @Transactional(readOnly = true)
    public List<PlanResponse> getOnlyAssignmentPlan(Long groupId, Long currentUserStudentNumber){
        log.info("getOnlyAssignmentPlan called for group: {} by user: {}", groupId, currentUserStudentNumber);

        //검증 로직
        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);

        List<Plan> planList = planRepository.findAllByStudyGroup_StudyIdAndHasAssignmentTrue(groupId);

        log.info("Found {} assignment-plans in StudyGroup: {}", planList.size(), groupId);

        List<PlanResponse> planResponses = planList.stream()
                .map(this::createResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} assignment plans", planResponses.size());

        return planResponses;
    }

    // 계획 상세 조회하기
    @Transactional(readOnly = true)
    public DetailPlanResponse getDetailPlan(Long groupId, Long planId, Long currentUserStudentNumber) {
        log.info("getDetailPlan called for plan: {} in group: {} by user: {}",
                planId, groupId, currentUserStudentNumber);

        //검증 로직
        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);

        log.info("Found plan: {} with title: {}", plan.getPlanId(), plan.getTitle());
        return createDetailPlanResponse(plan);
    }

    // 계획 수정하기 (멘토만 가능함.)
    @Transactional
    public DetailPlanResponse updatePlan(Long groupId, Long planId, PlanDto dto, Long currentUserStudentNumber) {
        log.info("updatePlan called for plan: {} in group: {} by user: {}",
                planId, groupId, currentUserStudentNumber);

        //검증 로직
        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);

        // 날짜 수정시 검증 추가
        if (dto.getStartDate() != null || dto.getEndDate() != null) {
            LocalDateTime newStartDate = dto.getStartDate() != null ? dto.getStartDate() : plan.getStartDate();
            LocalDateTime newEndDate = dto.getEndDate() != null ? dto.getEndDate() : plan.getEndDate();
            validationUtils.validatePlanDates(newStartDate, newEndDate);
        }

        log.info("Found plan: {} with title: {}", plan.getPlanId(), plan.getTitle());

        updatePlanData(plan, dto);
        return createDetailPlanResponse(plan);
    }

    // 계획 삭제하기 (멘토만 가능함.)
    @Transactional
    public void deletePlan(Long groupId, Long planId, Long currentUserStudentNumber) {
        log.info("deletePlan called for plan: {} in group: {} by user: {}",
                planId, groupId, currentUserStudentNumber);

        //검증 로직
        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);

        log.info("Deleting plan: {} with title: {}", plan.getPlanId(), plan.getTitle());

        planRepository.delete(plan);
        log.info("Plan deleted successfully: {}", planId);
    }

    // 계획 댓글 생성
    @Transactional
    public PlanCommentResponse createPlanComment(Long groupId, Long planId, Long currentUserStudentNumber, PlanCommentDto dto) {
        log.info("createPlanComment called for plan: {} in group: {} by user: {}",
                planId, groupId, currentUserStudentNumber);

        //검증 로직
        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);
        validationUtils.validateComment(dto.getContent()); // 댓글 내용 검증 강화

        PlanComment planComment = PlanComment.builder()
                .plan(plan)
                .author(user)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        planCommentRepository.save(planComment);
        log.info("Plan comment created successfully with ID: {}", planComment.getCommentId());

        return createPlanCommentResponse(planComment);
    }

    // 댓글 수정 기능
    @Transactional
    public PlanCommentResponse updatePlanComment(Long groupId, Long planId, Long commentId, Long currentUserStudentNumber, PlanCommentDto dto){
        log.info("updatePlanComment called for comment: {} in plan: {} group: {} by user: {}",
                commentId, planId, groupId, currentUserStudentNumber);

        //검증 로직
        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        validationUtils.validateComment(dto.getContent()); // 댓글 내용 검증 강화

        // 댓글 관리 권한 검증 (작성자 본인 + 멘토)
        PlanComment planComment = validationUtils.validateCommentManagementAccess(commentId, user.getUserId(), groupId);

        updateCommentData(planComment, dto);

        return createPlanCommentResponse(planComment);
    }

    // 댓글 삭제 기능
    @Transactional
    public void deletePlanComment(Long groupId, Long planId, Long commentId, Long currentUserStudentNumber){
        log.info("deletePlanComment called for comment: {} in plan: {} group: {} by user: {}",
                commentId, planId, groupId, currentUserStudentNumber);

        //검증 로직
        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);

        // 댓글 관리 권한 검증 (작성자 본인 + 멘토)
        PlanComment planComment = validationUtils.validateCommentManagementAccess(commentId, user.getUserId(), groupId);

        planCommentRepository.delete(planComment);

        log.info("Plan comment deleted successfully: {}", commentId);
    }

    /**
     * === 데이터 처리 메서드들 ===
     */

    // Plan 데이터를 업데이트
    private void updatePlanData(Plan plan, PlanDto dto) {
        boolean isUpdated = false;

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            plan.setTitle(dto.getTitle());
            isUpdated = true;
        }

        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            plan.setContent(dto.getContent());
            isUpdated = true;
        }

        if (dto.getStartDate() != null) {
            plan.setStartDate(dto.getStartDate());
            isUpdated = true;
        }

        if (dto.getEndDate() != null) {
            plan.setEndDate(dto.getEndDate());
            isUpdated = true;
        }

        // hasAssignment는 boolean이므로 항상 업데이트
        plan.setHasAssignment(dto.isHasAssignment());
        isUpdated = true;

        if (isUpdated) {
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);
            log.info("Plan updated successfully: {}", plan.getPlanId());
        } else {
            log.warn("No valid fields to update for plan: {}", plan.getPlanId());
        }
    }

    private void updateCommentData(PlanComment planComment, PlanCommentDto dto){
        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            planComment.setContent(dto.getContent());
            planComment.setUpdatedAt(LocalDateTime.now());

            planCommentRepository.save(planComment);
            log.info("Plan comment updated successfully: {}", planComment.getCommentId());
        } else {
            log.warn("No valid content to update for comment: {}", planComment.getCommentId());
        }
    }

    /**
     * === DTO 변환 메서드들 ===
     */

    // Plan를 DetailPlanResponse로 변환
    private DetailPlanResponse createDetailPlanResponse(Plan plan) {
        List<PlanComment> assignmentCommentList = planCommentRepository.findAllByPlanPlanId(plan.getPlanId());
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
    private PlanCommentResponse createPlanCommentResponse(PlanComment planComment) {
        return PlanCommentResponse.builder()
                .commentId(planComment.getCommentId())
                .content(planComment.getContent())
                .planId(planComment.getPlan().getPlanId())
                .creatorName(planComment.getAuthor().getName())
                .createdAt(planComment.getCreatedAt())
                .build();
    }
}