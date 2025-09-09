package com.mjsec.lms.domain.mentor;

import com.mjsec.lms.domain.study.BaseEntity;
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
@Table(name = "plan_comment")
@SuperBuilder
@SQLDelete(sql = "UPDATE plan_comment SET deleted_at = NOW() WHERE comment_id = ?")
@SQLRestriction("deleted_at is null")
public class PlanComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assign_id")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    /* 나중에 답글 개념까지 추가한다면 쓰일 변수
    @Builder.Default
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AssignmentComment> replies = new ArrayList<>();
     */
}