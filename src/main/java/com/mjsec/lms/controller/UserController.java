package com.mjsec.lms.controller;

import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.dto.UserResponse;
import com.mjsec.lms.service.UserService;
import com.mjsec.lms.type.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user-page")
    public ResponseEntity<SuccessResponse<UserResponse>> getUserPage(Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        UserResponse userResponse = userService.getUserPage(currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.USER_GET_PAGE_SUCCESS,
                        userResponse
                )
        );
    }
}
