package com.mjsec.lms.studygroup.domain;

import com.mjsec.lms.studygroup.domain.type.GroupMemberRole;
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
@Table(name = "group_member")
@SuperBuilder
@SQLDelete(sql = "UPDATE group_member SET deleted_at = NOW() WHERE member_id = ?")
@SQLRestriction("deleted_at is null")
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GroupMemberRole role = GroupMemberRole.MENTEE;

    @Builder.Default
    @Column(name = "warn", nullable = false)
    private Integer warn = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private StudyGroup studyGroup;
}