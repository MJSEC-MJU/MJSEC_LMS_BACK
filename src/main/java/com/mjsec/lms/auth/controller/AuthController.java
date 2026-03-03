package com.mjsec.lms.auth.controller;

import com.mjsec.lms.auth.dto.AuthDto;
import com.mjsec.lms.common.dto.SuccessResponse;
import com.mjsec.lms.auth.service.AuthService;
import com.mjsec.lms.common.type.ResponseMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-student-number")
    public ResponseEntity<SuccessResponse<Boolean>> checkStudentNumber(
            @RequestParam Long studentNumber
    ) {

        if(authService.checkStudentNumber(studentNumber)){
            return ResponseEntity.status(HttpStatus.OK).body(
                    SuccessResponse.of(
                            ResponseMessage.STUDENT_NUMBER_CHECK_SUCCESS,
                            true
                    )
            );
        }
        else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    SuccessResponse.of(
                            ResponseMessage.DUPLICATE_STUDENT_NUMBER,
                            false
                    )
            );
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<SuccessResponse<Boolean>> checkEmail(@RequestParam String email){

        if(authService.checkEmail(email)) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    SuccessResponse.of(
                            ResponseMessage.EMAIL_CHECK_SUCCESS,
                            true
                    )
            );
        }
        else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    SuccessResponse.of(
                            ResponseMessage.DUPLICATE_EMAIL,
                            false
                    )
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<Void>> register(
            @RequestBody @Valid AuthDto.Register registerRequest
    ){

        authService.register(registerRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.REGISTER_SUCCESS
                )
        );
    }
}
