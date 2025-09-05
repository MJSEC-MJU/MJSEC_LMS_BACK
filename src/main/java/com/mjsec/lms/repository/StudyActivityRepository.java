package com.mjsec.lms.repository;

import com.mjsec.lms.domain.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {

    @Query("SELECT sa FROM StudyActivity sa WHERE sa.studyGroup.studyId = :groupId")
    List<StudyActivity> findAllByStudyGroupId(@Param("groupId") Long groupId);
}
