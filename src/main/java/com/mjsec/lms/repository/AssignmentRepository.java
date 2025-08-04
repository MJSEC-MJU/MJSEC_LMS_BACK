package com.mjsec.lms.repository;

import com.mjsec.lms.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findAllByStudyGroup_StudyId(Long studyId);
}
