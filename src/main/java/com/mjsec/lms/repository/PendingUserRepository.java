package com.mjsec.lms.repository;

import com.mjsec.lms.domain.PendingUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {

    boolean existsByStudentNumber(Long studentNumber);

    boolean existsByEmail(String email);

    Optional<PendingUser> findByStudentNumber(Long studentNumber);
}
