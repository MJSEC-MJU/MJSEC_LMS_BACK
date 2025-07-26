package com.mjsec.lms.controller;


import com.mjsec.lms.dto.AssignmentDTO;
import com.mjsec.lms.dto.AssignmentResponse;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.AssignmentService;
import com.mjsec.lms.type.ResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/{groupId}/create-assignment")
    public ResponseEntity<SuccessResponse<AssignmentResponse>> createAssignment(
            @PathVariable Long groupId,
            @RequestBody AssignmentDTO dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        AssignmentResponse response = assignmentService.createAssignment(groupId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUCCESS,
                        response
                )
        );
    }

    @GetMapping("/{groupId}/assignments")
    public ResponseEntity<SuccessResponse<List<AssignmentResponse>>> getAssignments(
            @PathVariable Long groupId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        List<AssignmentResponse> assignmentResponseList = assignmentService.getAssignment(groupId,currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUCCESS,
                        assignmentResponseList
                )
        );
    }
}
