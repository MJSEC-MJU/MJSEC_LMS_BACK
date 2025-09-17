package com.mjsec.lms.controller;

import com.mjsec.lms.dto.PendingUserDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupRequestDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupResponseDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupUpdateDto;
import com.mjsec.lms.dto.StudyGroupSummaryDto;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.dto.UserAdminResponseDto;
import com.mjsec.lms.service.AdminService;
import com.mjsec.lms.type.ResponseMessage;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/member-refusal/{studentNumber}")
    public ResponseEntity<SuccessResponse<Void>> refuseRegister(@PathVariable Long studentNumber) {

        adminService.refuseRegister(studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.REFUSE_REGISTER_SUCCESS
                )
        );
    }

    @GetMapping("/group/all")
    public ResponseEntity<SuccessResponse<List<StudyGroupSummaryDto>>> getAllGroups() {

        List<StudyGroupSummaryDto> groups = adminService.getAllGroups();

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.GET_ALL_GROUPS_SUCCESS,
                        groups
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

    @PutMapping("/group/{name}")
    public ResponseEntity<SuccessResponse<Void>> updateGroup(
            @PathVariable String name,
            @RequestPart(required = false) MultipartFile studyImage,
            @Valid @RequestPart(required = false) StudyGroupUpdateDto studyGroupUpdateDto
    ) {

        adminService.updateGroup(name, studyImage, studyGroupUpdateDto);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.UPDATE_GROUP_SUCCESS
                )
        );
    }

    @GetMapping("/users")
    public ResponseEntity<SuccessResponse<List<UserAdminResponseDto>>> getAllUsersForAdmin() {

        List<UserAdminResponseDto> users = adminService.getAllUsersForAdmin();

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.GET_ALL_USER_INFO_SUCCESS,
                        users
                )
        );
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<SuccessResponse<Void>> deleteUser(@PathVariable("userId") Long userId) {

        adminService.deleteUser(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.DELETE_USER_SUCCESS
                )
        );
    }

    @PutMapping("/group/status/{groupId}")
    public ResponseEntity<SuccessResponse<Void>> updateGroupStatus(@PathVariable("groupId") Long groupId) {

        adminService.updateGroupStatus(groupId);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.UPDATE_GROUP_STATUS_SUCCESS
                )
        );
    }
}
