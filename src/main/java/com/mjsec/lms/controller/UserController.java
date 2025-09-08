package com.mjsec.lms.controller;

import com.mjsec.lms.dto.PasswordUpdateDto;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.dto.UserResponse;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.service.AuthCodeService;
import com.mjsec.lms.service.EmailService;
import com.mjsec.lms.service.UserService;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final AuthCodeService authCodeService;
    private final EmailService emailService;

    UserController(UserService userService, AuthCodeService authCodeService, EmailService emailService) {

        this.userService = userService;
        this.authCodeService = authCodeService;
        this.emailService = emailService;
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

    @PostMapping("/password/send-code")
    public ResponseEntity<SuccessResponse<Void>> sendCode(@RequestParam String email) {

        if(!userService.isValidEmail(email)) {
            throw new RestApiException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if(!userService.isEmailExists(email)) {
            throw new RestApiException(ErrorCode.NOT_REGISTERED_EMAIL);
        }

        String code = authCodeService.generateAndStoreCode(email);
        emailService.sendVerificationEmail(email, code);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.SEND_CODE_SUCCESS
                )
        );
    }

    @PostMapping("/password/verify-code")
    public ResponseEntity<SuccessResponse<Void>> verifyCode(
            @RequestParam String email,
            @RequestParam String code)
    {

        boolean isValid = authCodeService.verifyCode(email, code);

        if(isValid) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    SuccessResponse.of(
                            ResponseMessage.EMAIL_VERIFICATION_SUCCESS
                    )
            );
        }
        else {
            throw new RestApiException(ErrorCode.FAILED_VERIFICATION);
        }
    }

    @PutMapping("/password/update")
    public ResponseEntity<SuccessResponse<Void>> updatePassword(
            @RequestBody PasswordUpdateDto passwordUpdateDto)
    {

        userService.updatePassword(passwordUpdateDto.getEmail(), passwordUpdateDto.getPassword());

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.UPDATE_PASSWORD_SUCCESS
                )
        );
    }
}
