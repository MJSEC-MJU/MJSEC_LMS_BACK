package com.mjsec.lms.service;

import com.mjsec.lms.domain.*;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.type.GroupMemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WeeklyAlertService {

    private final PlanRepository planRepository;
    private final SubmissionRepository submissionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final EmailService emailService;

    public WeeklyAlertService(PlanRepository planRepository,
                              SubmissionRepository submissionRepository,
                              GroupMemberRepository groupMemberRepository,
                              EmailService emailService) {
        this.planRepository = planRepository;
        this.submissionRepository = submissionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.emailService = emailService;
    }

    //주간 통합 과제 미제출자 체크 및 알림 발송
    @Transactional(readOnly = true)
    public void checkAndSendWeeklyAssignmentReport() {
        log.info("Starting weekly assignment report generation...");

        LocalDateTime now = LocalDateTime.now();

        // 마감된 모든 과제들 조회
        List<Plan> expiredPlans = planRepository.findAll().stream()
                .filter(assignment -> assignment.getEndDate() != null && assignment.getEndDate().isBefore(now))
                .collect(Collectors.toList());

        if (expiredPlans.isEmpty()) {
            log.info("No expired assignments found. Weekly report will not be sent.");
            return;
        }

        log.info("Found {} expired assignments to process", expiredPlans.size());

        // 스터디 그룹별로 과제들을 그룹핑하고 미제출 정보 생성
        Map<String, List<AssignmentNotSubmittedInfo>> reportData = new HashMap<>();

        Map<StudyGroup, List<Plan>> assignmentsByStudyGroup = expiredPlans.stream()
                .collect(Collectors.groupingBy(Plan::getStudyGroup));

        for (Map.Entry<StudyGroup, List<Plan>> entry : assignmentsByStudyGroup.entrySet()) {
            StudyGroup studyGroup = entry.getKey();
            List<Plan> plans = entry.getValue();

            List<AssignmentNotSubmittedInfo> notSubmittedInfos = generateNotSubmittedInfos(studyGroup, plans);

            if (!notSubmittedInfos.isEmpty()) {
                reportData.put(studyGroup.getName(), notSubmittedInfos);
            }
        }

        if (reportData.isEmpty()) {
            log.info("All assignments have been submitted. Weekly report will not be sent.");
            return;
        }

        // 이메일 발송
        emailService.sendWeeklyAssignmentReport(reportData, now);
        log.info("Weekly assignment report sent successfully for {} study groups", reportData.size());
    }

    //특정 스터디 그룹의 과제 미제출 정보 생성
    private List<AssignmentNotSubmittedInfo> generateNotSubmittedInfos(StudyGroup studyGroup, List<Plan> plans) {
        log.info("Generating report for study group: {} with {} assignments",
                studyGroup.getName(), plans.size());

        // 해당 스터디 그룹의 모든 멘티 조회
        List<GroupMember> mentees = groupMemberRepository.findAll().stream()
                .filter(member -> member.getStudyGroup().getStudyId().equals(studyGroup.getStudyId()))
                .filter(member -> member.getRole() == GroupMemberRole.MENTEE)
                .collect(Collectors.toList());

        List<AssignmentNotSubmittedInfo> result = new ArrayList<>();

        // 각 과제별로 미제출자 확인
        for (Plan plan : plans) {
            // 과제를 제출한 사용자 ID 목록
            List<Long> submittedUserIds = submissionRepository
                    .findAssignmentSubmissionsByAssignmentAssignId(plan.getPlanId())
                    .stream()
                    .map(submission -> submission.getSubmitter().getUserId())
                    .collect(Collectors.toList());

            // 미제출자 필터링
            List<String> notSubmittedStudents = mentees.stream()
                    .filter(mentee -> !submittedUserIds.contains(mentee.getUser().getUserId()))
                    .map(mentee -> mentee.getUser().getName() + " (" + mentee.getUser().getStudentNumber() + ")")
                    .collect(Collectors.toList());

            if (!notSubmittedStudents.isEmpty()) {
                AssignmentNotSubmittedInfo info = AssignmentNotSubmittedInfo.builder()
                        .planTitle(plan.getTitle())
                        .endDate(plan.getEndDate())
                        .notSubmittedStudents(notSubmittedStudents)
                        .build();

                result.add(info);
                log.info("Assignment '{}' has {} not submitted students",
                        plan.getTitle(), notSubmittedStudents.size());
            }
        }

        return result;
    }
}