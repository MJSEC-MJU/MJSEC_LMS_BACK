package com.mjsec.lms.repository;

import com.mjsec.lms.domain.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {
}
