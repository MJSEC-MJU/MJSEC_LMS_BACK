package com.mjsec.lms.controller;

import com.mjsec.lms.dto.ImageResponse;
import com.mjsec.lms.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/image")
@Slf4j
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /*
    이미지 파일 서빙 API
     * 인증된 사용자만 접근 가능하며, 스터디 그룹 멤버십 확인 후 이미지 반환
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String filename,
            Authentication authentication) {

        log.info("Image request received for filename: {}", filename);

        // JwtFilter에서 설정한 studentNumber를 가져옴
        Long currentUserStudentNumber = (Long) authentication.getPrincipal();

        // 이미지 파일과 메타데이터 조회
        ImageResponse imageResponse = imageService.getImage(filename, currentUserStudentNumber);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(imageResponse.getMediaType());
        headers.setContentLength(imageResponse.getContentLength());

        // 브라우저 캐싱 설정 (1시간)
        headers.setCacheControl("public, max-age=3600");

        // 파일명 설정 (다운로드 시 사용될 이름)
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.inline()
                        .filename(imageResponse.getOriginalFilename())
                        .build()
        );

        log.info("Image served successfully: {}, size: {} bytes",
                filename, imageResponse.getContentLength());

        return ResponseEntity.ok()
                .headers(headers)
                .body(imageResponse.getResource());
    }
    
}