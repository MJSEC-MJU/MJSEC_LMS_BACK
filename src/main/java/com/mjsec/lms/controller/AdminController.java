package com.mjsec.lms.controller;

import com.mjsec.lms.dto.PendingUserDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupRequestDto;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.AdminService;
import com.mjsec.lms.type.ResponseMessage;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/member-approval")
    public ResponseEntity<SuccessResponse<List<PendingUserDto>>> getAllPendingUser(){

        List<PendingUserDto> pendingUsers = adminService.getAllPendingUser();

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.GET_ALL_PENDING_USER_SUCCESS,
                        pendingUsers
                )
        );
    }

    @PostMapping("/member-approval/{studentNumber}")
    public ResponseEntity<SuccessResponse<Void>> approveRegister(@PathVariable Long studentNumber) {

        adminService.approveRegister(studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.APPROVE_REGISTER_SUCCESS
                )
        );
    }

    @PostMapping("/group")
    public ResponseEntity<SuccessResponse<Void>> createGroup(@Valid @RequestBody StudyGroupRequestDto studyGroupDto){

        adminService.createGroup(studyGroupDto);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.CREATE_GROUP_SUCCESS
                )
        );
    }
}
