package com.mjsec.lms.repository.study;

import com.mjsec.lms.domain.study.StudyGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    boolean existsByName(String name);

    Optional<StudyGroup> findByStudyId(Long studyId);
}
