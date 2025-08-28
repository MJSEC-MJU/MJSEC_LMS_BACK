package com.mjsec.lms.service;

import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.UserResponse;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ValidationUtils validationUtils;

    public UserService(UserRepository userRepository, ValidationUtils validationUtils) {

        this.userRepository = userRepository;
        this.validationUtils = validationUtils;
    }

    //유저 페이지 조회
    public UserResponse getUserPage(Long studentNumber) {

        log.info("get user page called with studentNumber: {}", studentNumber);

        User user = validationUtils.validateUser(studentNumber);

        return createUserResponse(user);
    }

    //UserResponse 반환하기
    private UserResponse createUserResponse(User user) {

        return UserResponse.builder()
                .userId(user.getUserId())
                .studentNumber(user.getStudentNumber())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImage(user.getProfileImage())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
