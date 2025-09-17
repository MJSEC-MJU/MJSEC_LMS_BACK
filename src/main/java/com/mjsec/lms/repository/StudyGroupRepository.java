package com.mjsec.lms.repository;

import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    boolean existsByName(String name);

    Optional<StudyGroup> findByName(String name);

    Optional<StudyGroup> findByStudyId(Long studyId);

    @Modifying
    @Query("DELETE FROM StudyGroup s WHERE s.creator = :user")
    void deleteByCreator(@Param("user") User user);
}
