package com.mjsec.lms.repository.user;

import com.mjsec.lms.domain.user.PendingUser;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {

    boolean existsByStudentNumber(Long studentNumber);

    boolean existsByEmail(String email);

    Optional<PendingUser> findByStudentNumber(Long studentNumber);
}
