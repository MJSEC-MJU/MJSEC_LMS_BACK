package com.mjsec.lms.domain.mentor;

import com.mjsec.lms.domain.study.AssignmentSubmission;
import com.mjsec.lms.domain.study.BaseEntity;
import com.mjsec.lms.domain.study.StudyGroup;
import com.mjsec.lms.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plan")
@SuperBuilder
@SQLDelete(sql = "UPDATE plan SET deleted_at = NOW() WHERE plan_id = ?")
@SQLRestriction("deleted_at is null")
public class Plan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private boolean hasAssignment = false;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private StudyGroup studyGroup;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AssignmentSubmission> submissions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PlanComment> comments = new ArrayList<>();
}