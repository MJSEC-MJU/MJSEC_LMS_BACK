package com.mjsec.lms.announcement.repository;

import com.mjsec.lms.announcement.domain.Announcement;
import com.mjsec.lms.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Modifying
    @Query("DELETE FROM Announcement a WHERE a.creator = :user")
    void deleteByCreator(@Param("user") User user);
}
