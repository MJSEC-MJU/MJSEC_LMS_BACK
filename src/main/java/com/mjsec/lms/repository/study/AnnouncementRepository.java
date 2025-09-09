package com.mjsec.lms.repository.study;

import com.mjsec.lms.domain.study.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
}
