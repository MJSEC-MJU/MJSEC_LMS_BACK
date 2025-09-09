package com.mjsec.lms.domain.mentor;

import com.mjsec.lms.domain.study.BaseEntity;
import com.mjsec.lms.domain.study.StudyActivity;
import com.mjsec.lms.domain.study.StudyGroup;
import com.mjsec.lms.domain.user.User;
import com.mjsec.lms.type.AttendanceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance")
@SuperBuilder
@SQLDelete(sql = "UPDATE attendance SET deleted_at = NOW() WHERE attendance_id = ?")
@SQLRestriction("deleted_at is null")
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private StudyActivity studyActivity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceType type;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    private String week;
}
