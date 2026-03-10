package com.mjsec.lms.user.repository;

import com.mjsec.lms.user.domain.User;

import java.util.List;
import java.util.Optional;

import com.mjsec.lms.user.domain.type.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByStudentNumber(Long studentNumber);

    boolean existsByEmail(String email);

    Optional<User> findByStudentNumber(Long studentNumber);

    //역할로 유저 찾기
    List<User> findByRole(UserRole role);

    //역할이 ADMIN인 첫 번째 사용자 조회
    Optional<User> findFirstByRole(UserRole role);

    //모든 관리자의 이메일 주소만 조회
    @Query("SELECT u.email FROM User u WHERE u.role = :role")
    List<String> findEmailsByRole(UserRole role);

    Optional<User> findByUserId(Long userId);

    Optional<User> findByEmail(String email);
}
