package com.mjsec.lms.controller;

import com.mjsec.lms.dto.ImageResponse;
import com.mjsec.lms.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/image")
@Slf4j
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 이미지 파일 서빙 API
     * - JwtFilter가 공개로 스킵되어 Authentication이 null일 수 있으므로 널-세이프 처리
     * - 파일명에 '.' 포함되도록 패턴 사용
     */
    @GetMapping(value = "/{filename:.+}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<Resource> getImage(
            @PathVariable String filename
    ) {

        // 서비스에 권한/멤버십 검증은 그대로 위임
        ImageResponse imageResponse = imageService.getImage(filename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(imageResponse.getMediaType());
        headers.setContentLength(imageResponse.getContentLength());
        headers.setCacheControl("public, max-age=3600"); // 1시간 캐시
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
