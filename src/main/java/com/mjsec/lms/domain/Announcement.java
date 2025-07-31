package com.mjsec.lms.domain;

import jakarta.persistence.*;
import lombok.*;
import com.mjsec.lms.type.AnnouncementType;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long announcementId;

    private Long userId;

    @Column(nullable = false, length= 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AnnouncementType type;

    private LocalDateTime startDate;
    private LocalDateTime endDate;






}
