package com.mjsec.lms.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentNotSubmittedInfo {

    private  String planTitle;
    private  LocalDateTime endDate;
    private  List<String> notSubmittedStudents;

    //미제출자가 있는지 확인
    public boolean hasNotSubmittedStudents() {
        return notSubmittedStudents != null && !notSubmittedStudents.isEmpty();
    }

    //미제출자 수 반환
    public int getNotSubmittedCount() {
        return notSubmittedStudents != null ? notSubmittedStudents.size() : 0;
    }

    //마감일 지난 여부 확인
    public boolean isExpired(LocalDateTime currentTime) {
        return endDate != null && endDate.isBefore(currentTime);
    }

}
