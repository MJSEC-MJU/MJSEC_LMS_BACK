package com.mjsec.lms.controller;


import com.mjsec.lms.dto.*;
import com.mjsec.lms.service.AssignmentService;
import com.mjsec.lms.type.ResponseMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    //과제 등록하기
    @PostMapping("/{groupId}/create-assignment")
    public ResponseEntity<SuccessResponse<DetailAssignmentResponse>> createAssignment(
            @PathVariable Long groupId,
            @Valid @RequestBody AssignmentDto dto,
            Authentication authentication) {  // Spring Security가 자동 주입

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailAssignmentResponse response = assignmentService.createAssignment(groupId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_CREATE_SUCCESS,
                        response
                )
        );
    }

    //전체 과제 조회하기
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

    //과제 상세 조회하기
    @GetMapping("/{groupId}/assignments/{assignId}")
    public ResponseEntity<SuccessResponse<DetailAssignmentResponse>> getDetailedAssignment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailAssignmentResponse detailAssignmentResponse = assignmentService.getDetailAssignment(groupId,assignId,currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUCCESS,
                        detailAssignmentResponse
                )
        );
    }

    //등록한 과제 수정하기
    @PutMapping("/{groupId}/assignments/{assignId}")
    public ResponseEntity<SuccessResponse<DetailAssignmentResponse>> updateAssignment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            @RequestBody AssignmentDto dto,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        DetailAssignmentResponse detailAssignmentResponse = assignmentService.updateAssignment(groupId, assignId, dto, currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_UPDATE_SUCCESS,
                        detailAssignmentResponse
                )
        );
    }

    //등록한 과제 삭제하기
    @DeleteMapping("/{groupId}/assignments/{assignId}")
    public ResponseEntity<SuccessResponse<Void>> deleteAssignment(
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        assignmentService.deleteAssignment(groupId,assignId,currentUserStudentNumber);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_DELETE_SUCCESS
                )
        );
    }

    //과제 제출하기 (멘티가)
    @PostMapping("/{groupId}/assign-submit/{assignId}")
    public ResponseEntity<SuccessResponse<SubmissionResponse>> submitAssignment(
            @Valid @RequestBody SubmissionDto dto,
            @PathVariable Long groupId,
            @PathVariable Long assignId,
            Authentication authentication,
            HttpServletRequest request){

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        //클라이언트 IP 뽑아내기
        String clientIpAddr = extractClientIp(request);

        SubmissionResponse submissionResponse = assignmentService.submitAssignment(groupId,assignId,currentUserStudentNumber,dto,clientIpAddr);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        ResponseMessage.ASSIGNMENT_SUBMIT_SUCCESS,
                        submissionResponse
                )
        );
    }

    //클라이언트 IP 뽑아내기
    private String extractClientIp(HttpServletRequest request) {

        String clientIp = null;

        // X-Forwarded-For 헤더 확인
        clientIp = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(clientIp) && !isUnknownIp(clientIp)) {
            return clientIp.split(",")[0].trim();
        }

        // Proxy-Client-IP 헤더 확인
        clientIp = request.getHeader("Proxy-Client-IP");
        if (StringUtils.hasText(clientIp) && !isUnknownIp(clientIp)) {
            return clientIp;
        }

        // WL-Proxy-Client-IP 헤더 확인
        clientIp = request.getHeader("WL-Proxy-Client-IP");
        if (StringUtils.hasText(clientIp) && !isUnknownIp(clientIp)) {
            return clientIp;
        }

        // HTTP_CLIENT_IP 헤더 확인
        clientIp = request.getHeader("HTTP_CLIENT_IP");
        if (StringUtils.hasText(clientIp) && !isUnknownIp(clientIp)) {
            return clientIp;
        }

        // HTTP_X_FORWARDED_FOR 헤더 확인
        clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (StringUtils.hasText(clientIp) && !isUnknownIp(clientIp)) {
            return clientIp;
        }

        // 기본적으로 getRemoteAddr() 사용
        return request.getRemoteAddr();
    }

    //알 수 없는 IP인지 확인하기
    private boolean isUnknownIp(String ip) {

        return "unknown".equalsIgnoreCase(ip) ||
                "0:0:0:0:0:0:0:1".equals(ip) ||
                "127.0.0.1".equals(ip);
    }

}
