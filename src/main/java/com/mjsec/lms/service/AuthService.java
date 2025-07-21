package com.mjsec.lms.service;

import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.PendingUserRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
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

        // 학번이 8자리 숫자인지 체크
        if(!String.valueOf(studentNumber).matches("^\\d{8}$")){
            throw new RestApiException(ErrorCode.INVALID_STUDENT_NUMBER);
        }

        // 학번이 회원가입 승인 대기 중인 테이블 & 유저 테이블에 등록되어 있는지 확인
        if(pendingUserRepository.existsByStudentNumber(studentNumber)) return false;
        if(userRepository.existsByStudentNumber(studentNumber)) return false;

        return true;
    }
}
