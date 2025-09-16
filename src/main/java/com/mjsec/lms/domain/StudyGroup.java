package com.mjsec.lms.domain;

import com.mjsec.lms.type.StudyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "study_group")
@SuperBuilder
@SQLDelete(sql = "UPDATE study_group SET deleted_at = NOW() WHERE study_id = ?")
@SQLRestriction("deleted_at is null")
public class StudyGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Long studyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "study_image", columnDefinition = "TEXT")
    private String studyImage;

    private StudyStatus status = StudyStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User creator;

    @Builder.Default
    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Plan> plans = new ArrayList<>();
}