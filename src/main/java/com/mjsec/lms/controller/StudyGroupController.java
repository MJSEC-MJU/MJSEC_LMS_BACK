package com.mjsec.lms.controller;

import com.mjsec.lms.dto.StudyActivityDto;
import com.mjsec.lms.dto.StudyActivityResponse;
import com.mjsec.lms.dto.SuccessResponse;
import com.mjsec.lms.service.StudyGroupService;
import com.mjsec.lms.type.ResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    StudyGroupController(StudyGroupService studyGroupService){

        this.studyGroupService = studyGroupService;
    }

    @PostMapping("/{groupId}/create-activity")
    public ResponseEntity<SuccessResponse<StudyActivityResponse>> createStudyActivity(
            @PathVariable Long groupId,
            @RequestBody StudyActivityDto studyActivityDto,
            Authentication authentication
    ){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        StudyActivityResponse studyActivityResponse = studyGroupService.createStudyActivity(groupId, currentUserStudentNumber, studyActivityDto);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.STUDY_ACTIVITY_SUCCESS,
                        studyActivityResponse
                )
        );
    }
}
