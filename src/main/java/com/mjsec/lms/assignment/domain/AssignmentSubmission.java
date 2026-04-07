package com.mjsec.lms.assignment.domain;

import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.mjsec.lms.common.domain.BaseEntity;
import com.mjsec.lms.user.domain.User;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assignment_submission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_submitter_plan", columnNames = {"user_id", "plan_id"})
})
@SuperBuilder
@SQLDelete(sql = "UPDATE assignment_submission SET deleted_at = NOW() WHERE submission_id = ?")
@SQLRestriction("deleted_at is null")
public class AssignmentSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    //혹시 모를 IPv6를 위한 45자 설정
    @Column(name = "submitter_ip", length = 45)
    private String submitterIp;

    //블로그 비밀번호 (Tistory)
    @Column(name="password", columnDefinition = "TEXT")
    private String password;
    
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    // 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;
}