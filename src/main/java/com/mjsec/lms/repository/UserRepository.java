package com.mjsec.lms.repository;

import com.mjsec.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByStudentNumber(Long studentNumber);
}
