package com.mjsec.lms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionStatisticsResponse {

    private int totalMentees;           // 전체 멘티 수
    private int submittedCount;         // 제출 완료 상태
    private int completedCount;         // 과제 완료 상태
    private int revisionRequiredCount;  // 수정 필요 상태
    private int notSubmittedCount;      // 미제출 수

}