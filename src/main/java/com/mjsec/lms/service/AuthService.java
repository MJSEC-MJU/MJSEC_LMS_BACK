package com.mjsec.lms.service;

import com.mjsec.lms.domain.PendingUser;
import com.mjsec.lms.dto.AuthDto;
import com.mjsec.lms.dto.UserDto;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.PendingUserRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(PendingUserRepository pendingUserRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {

        this.pendingUserRepository = pendingUserRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 사용 중인 학번인지 체크하는 메소드 (회원가입 승인 대기 중인 테이블까지 확인)
     * @param studentNumber 학번
     * @return true / false
     */
    public Boolean checkStudentNumber(Long studentNumber){

        // 학번이 8자리 숫자인지 체크
        if(studentNumber == null || !String.valueOf(studentNumber).matches("^\\d{8}$")){
            throw new RestApiException(ErrorCode.INVALID_STUDENT_NUMBER);
        }

        // 학번이 회원가입 승인 대기 중인 테이블 & 유저 테이블에 등록되어 있는지 확인
        if(pendingUserRepository.existsByStudentNumber(studentNumber)) return false;
        if(userRepository.existsByStudentNumber(studentNumber)) return false;

        return true;
    }

    /**
     * 사용 중인 이메일인지 체크하는 메소드 (회원가입 승인 대기 중인 테이블까지 확인)
     * @param email 이메일
     * @return true / false
     */
    public Boolean checkEmail(String email){

        // 이메일 형식에 맞는지 체크
        if(email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")){
            throw new RestApiException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        // 이메일이 회원가입 승인 대기 중인 테이블 & 유저 테이블에 등록되어 있는지 확인
        if(pendingUserRepository.existsByEmail(email)) return false;
        if(userRepository.existsByEmail(email)) return false;

        return true;
    }


    /**
     * 회원가입 승인 요청을 처리하는 메소드
     * @param registerRequest 유저의 개인 정보를 담은 Dto
     */
    public void register(AuthDto.Register registerRequest){

        UserDto userDto = registerRequest.getUserDto();

        // 학번 중복 체크
        if(pendingUserRepository.existsByStudentNumber(userDto.getStudentNumber())){
            throw new RestApiException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
        if(userRepository.existsByStudentNumber(userDto.getStudentNumber())){
            throw new RestApiException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }

        // 이메일 중복 체크
        if(pendingUserRepository.existsByEmail(userDto.getEmail())){
            throw new RestApiException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new RestApiException(ErrorCode.DUPLICATE_EMAIL);
        }

        PendingUser pendingUser = PendingUser.builder()
                .studentNumber(userDto.getStudentNumber())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .name(userDto.getName())
                .email(userDto.getEmail())
                .phoneNumber(userDto.getPhoneNumber())
                .build();

        pendingUserRepository.save(pendingUser);
    }
}
