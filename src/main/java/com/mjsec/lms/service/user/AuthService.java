package com.mjsec.lms.service.user;

import com.mjsec.lms.domain.user.PendingUser;
import com.mjsec.lms.dto.user.AuthDto;
import com.mjsec.lms.dto.user.UserDto;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.user.PendingUserRepository;
import com.mjsec.lms.repository.user.UserRepository;
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
            log.warn("Invalid student number format: {}", studentNumber);
            throw new RestApiException(ErrorCode.INVALID_STUDENT_NUMBER);
        }

        // 학번이 회원가입 승인 대기 중인 테이블 & 유저 테이블에 등록되어 있는지 확인
        if(pendingUserRepository.existsByStudentNumber(studentNumber)) {
            log.info("Student number {} already exists in PendingUser", studentNumber);
            return false;
        }
        if(userRepository.existsByStudentNumber(studentNumber)) {
            log.info("Student number {} already exists in User", studentNumber);
            return false;
        }

        log.info("Student number {} is available", studentNumber);
        return true;
    }

    /**
     * 사용 중인 이메일인지 체크하는 메소드 (회원가입 승인 대기 중인 테이블까지 확인)
     * @param email 이메일
     * @return true / false
     */
    public Boolean checkEmail(String email){

        log.info("checkEmail called with email: {}", email);

        // 이메일 형식에 맞는지 체크
        if(email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")){
            log.warn("Invalid email format: {}", email);
            throw new RestApiException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        // 이메일이 회원가입 승인 대기 중인 테이블 & 유저 테이블에 등록되어 있는지 확인
        if(pendingUserRepository.existsByEmail(email)) {
            log.info("Email {} already exists in PendingUser", email);
            return false;
        }
        if(userRepository.existsByEmail(email)) {
            log.info("Email {} already exists in User", email);
            return false;
        }

        log.info("Email {} is available", email);
        return true;
    }


    /**
     * 회원가입 승인 요청을 처리하는 메소드
     * @param registerRequest 유저의 개인 정보를 담은 Dto (학번, 이름, 비밀번호, 이메일, 전화번호)
     */
    public void register(AuthDto.Register registerRequest){

        log.info("register called with studentNumber: {}", registerRequest.getUserDto().getStudentNumber());

        UserDto userDto = registerRequest.getUserDto();

        // 학번 중복 체크
        if(pendingUserRepository.existsByStudentNumber(userDto.getStudentNumber())){
            log.warn("Duplicate student number in PendingUser: {}", userDto.getStudentNumber());
            throw new RestApiException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
        if(userRepository.existsByStudentNumber(userDto.getStudentNumber())){
            log.warn("Duplicate student number in User: {}", userDto.getStudentNumber());
            throw new RestApiException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }

        // 이메일 중복 체크
        if(pendingUserRepository.existsByEmail(userDto.getEmail())){
            log.warn("Duplicate email in PendingUser: {}", userDto.getEmail());
            throw new RestApiException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(userRepository.existsByEmail(userDto.getEmail())){
            log.warn("Duplicate email in User: {}", userDto.getEmail());
            throw new RestApiException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 회원의 정보를 User 테이블이 아닌 승인 요청을 저장하는 PendingUser 테이블에 (임시) 저장
        PendingUser pendingUser = PendingUser.builder()
                .studentNumber(userDto.getStudentNumber())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .name(userDto.getName())
                .email(userDto.getEmail())
                .phoneNumber(userDto.getPhoneNumber())
                .build();

        pendingUserRepository.save(pendingUser);

        log.info("Pending user saved successfully: studentNumber = {}", pendingUser.getStudentNumber());
    }
}
