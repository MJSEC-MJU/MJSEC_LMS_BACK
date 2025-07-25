package com.mjsec.lms.controller;


import com.mjsec.lms.dto.AssignmentDTO;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.AssignmentService;
import com.mjsec.lms.type.ResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/{groupId}/assignments")
    public ResponseEntity<SuccessResponse<AssignmentDTO>> createAssignment(
            @PathVariable Long groupId,
            @RequestBody AssignmentDTO dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentService.createAssignment(groupId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUCCESS,
                        dto
                )
        );
    }

}
