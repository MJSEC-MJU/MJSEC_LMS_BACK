package com.mjsec.lms.announcement.domain;

import jakarta.persistence.*;
import lombok.*;
import com.mjsec.lms.announcement.domain.type.AnnouncementRole;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.mjsec.lms.common.domain.BaseEntity;
import com.mjsec.lms.user.domain.User;
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

    @Column(nullable = false, length= 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AnnouncementRole type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;
}
