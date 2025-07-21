package com.mjsec.lms.service;

import com.mjsec.lms.repository.PendingUserRepository;
import com.mjsec.lms.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;

    public AuthService(PendingUserRepository pendingUserRepository, UserRepository userRepository) {

        this.pendingUserRepository = pendingUserRepository;
        this.userRepository = userRepository;
    }

    /**
     * 사용 중인 학번인지 체크하는 메소드 (회원가입 승인 대기 중인 테이블까지 확인)
     * @param studentNumber 학번
     * @return true / false
     */
    public Boolean checkStudentNumber(Long studentNumber){

        if(pendingUserRepository.existsByStudentNumber(studentNumber)) return false;
        if(userRepository.existsByStudentNumber(studentNumber)) return false;

        return true;
    }
}
