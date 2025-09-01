package com.mjsec.lms.controller;

import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.service.MentorService;
import com.mjsec.lms.type.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mentor")
public class MentorController {

    private final MentorService mentorService;

    @PostMapping("/group/{groupId}/add-member/{studentNumber}")
    public ResponseEntity<SuccessResponse<Void>> addMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("studentNumber") Long studentNumber,
            Authentication authentication)
    {

        Long currentStudentNumber = (Long) authentication.getPrincipal();
        mentorService.addMember(currentStudentNumber, groupId, studentNumber);

        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.of(
                        ResponseMessage.ADD_MEMBER_SUCCESS
                )
        );
    }
}
