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

    // 주간 통합 과제 미제출자 체크 및 알림 발송 (최근 1주일간 마감된 과제만)
    @Transactional(readOnly = true)
    public void checkAndSendWeeklyAssignmentReport() {
        log.info("Starting weekly assignment report generation...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);

        // hasAssignment=true이고 최근 1주일간 마감된 과제만 조회
        List<Plan> recentlyExpiredAssignments = planRepository.findAssignmentsExpiredBetween(oneWeekAgo, now);

        if (recentlyExpiredAssignments.isEmpty()) {
            log.info("No recently expired assignments found in the past week. Weekly report will not be sent.");
            return;
        }

        log.info("Found {} recently expired assignments to process (past 7 days, assignments only)",
                recentlyExpiredAssignments.size());

        // 스터디 그룹별로 과제들을 그룹핑하고 미제출 정보 생성
        Map<String, List<AssignmentNotSubmittedInfo>> reportData = new HashMap<>();

        Map<StudyGroup, List<Plan>> assignmentsByStudyGroup = recentlyExpiredAssignments.stream()
                .collect(Collectors.groupingBy(Plan::getStudyGroup));

        for (Map.Entry<StudyGroup, List<Plan>> entry : assignmentsByStudyGroup.entrySet()) {
            StudyGroup studyGroup = entry.getKey();
            List<Plan> assignments = entry.getValue();

            List<AssignmentNotSubmittedInfo> notSubmittedInfos = generateNotSubmittedInfos(studyGroup, assignments);

            if (!notSubmittedInfos.isEmpty()) {
                reportData.put(studyGroup.getName(), notSubmittedInfos);
            }
        }

        if (reportData.isEmpty()) {
            log.info("All recently expired assignments have been submitted. Weekly report will not be sent.");
            return;
        }

        // 이메일 발송
        emailService.sendWeeklyAssignmentReport(reportData, now);
        log.info("Weekly assignment report sent successfully for {} study groups with recently expired assignments",
                reportData.size());
    }

    // 특정 스터디 그룹의 과제 미제출 정보 생성
    private List<AssignmentNotSubmittedInfo> generateNotSubmittedInfos(StudyGroup studyGroup, List<Plan> assignments) {
        log.info("Generating report for study group: {} with {} recently expired assignments",
                studyGroup.getName(), assignments.size());

        // 해당 스터디 그룹의 모든 멘티 조회
        List<GroupMember> mentees = groupMemberRepository.findAll().stream()
                .filter(member -> member.getStudyGroup().getStudyId().equals(studyGroup.getStudyId()))
                .filter(member -> member.getRole() == GroupMemberRole.MENTEE)
                .collect(Collectors.toList());

        List<AssignmentNotSubmittedInfo> result = new ArrayList<>();

        // 각 과제별로 미제출자 확인 (hasAssignment=true인 것들만 처리됨)
        for (Plan assignment : assignments) {
            // 과제를 제출한 사용자 ID 목록
            List<Long> submittedUserIds = submissionRepository
                    .findAssignmentSubmissionsByPlanPlanId(assignment.getPlanId())
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
                        .planTitle(assignment.getTitle())
                        .endDate(assignment.getEndDate())
                        .notSubmittedStudents(notSubmittedStudents)
                        .build();

                result.add(info);
                log.info("Recently expired assignment '{}' has {} not submitted students",
                        assignment.getTitle(), notSubmittedStudents.size());
            }
        }

        return result;
    }
}