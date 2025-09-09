package com.mjsec.lms.domain.study;

import com.mjsec.lms.domain.mentor.Plan;
import com.mjsec.lms.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assignment_submission")
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

    // 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;
}