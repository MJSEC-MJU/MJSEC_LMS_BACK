package com.mjsec.lms.service;

import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.type.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    // application.yml에서 파일 업로드 경로 설정값을 가져옴 (기본값: uploads/)
    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    // 최대 파일 크기 설정 (기본값: 10MB)
    @Value("${file.upload.max-size:10485760}") // 10MB
    private long maxFileSize;

    // 허용되는 이미지 파일 타입 목록
    private final List<String> allowedImageTypes = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 스프링 빈 생성 후 실행되는 초기화 메서드
    @PostConstruct
    public void init() {
        try {
            // 업로드 디렉토리가 존재하지 않으면 생성
            Files.createDirectories(Paths.get(uploadPath));
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", uploadPath, e);
        }
    }

    // 이미지 파일을 업로드하고 웹에서 접근 가능한 URL을 반환하는 메서드
    public String uploadImage(MultipartFile file) {
        // 파일 유효성 검사 수행
        validateImageFile(file);

        // 고유한 파일명 생성
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String filePath = uploadPath + fileName;

        try {
            Path targetLocation = Paths.get(filePath);
            // 파일을 지정된 경로에 저장 (기존 파일이 있으면 덮어쓰기)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {}", fileName);
            // 웹에서 접근 가능한 URL 형태로 반환
            return "/uploads/" + fileName;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", fileName, e);
            throw new RestApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 이미지를 업데이트하는 통합 메서드
    public String updateImage(String currentImageUrl, MultipartFile newImage, String entityName, Long entityId) {

        // 새 이미지가 업로드된 경우
        if (newImage != null && !newImage.isEmpty()) {
            log.info("New image uploaded, processing image replacement for {}: {}", entityName, entityId);

            // 기존 이미지가 있다면 삭제
            if (currentImageUrl != null && !currentImageUrl.trim().isEmpty()) {
                try {
                    deleteImage(currentImageUrl);
                    log.info("Previous image deleted successfully for {}: {}", entityName, currentImageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete previous image: {}, but continuing with new image upload for {}: {}",
                            currentImageUrl, entityName, entityId, e);
                }
            }

            // 새 이미지 업로드
            try {
                String newImageUrl = uploadImage(newImage);
                log.info("New image uploaded successfully for {}: {}", entityName, newImageUrl);
                return newImageUrl;
            } catch (Exception e) {
                log.error("Failed to upload new image for {}: {}", entityName, entityId, e);
                // 새 이미지 업로드 실패시 예외를 다시 던져서 트랜잭션 롤백 유도
                throw e;
            }
        }

        // 새 이미지가 없는 경우: 기존 이미지 URL 유지
        log.info("No new image provided for {}: {}, keeping existing image: {}", entityName, entityId, currentImageUrl);
        return currentImageUrl;
    }

    // 업로드할 이미지 파일의 유효성을 검사하는 메서드
    private void validateImageFile(MultipartFile file) {
        // 빈 파일인지 확인
        if (file.isEmpty()) {
            throw new RestApiException(ErrorCode.EMPTY_FILE);
        }

        // 파일 크기가 허용 범위를 초과하는지 확인
        if (file.getSize() > maxFileSize) {
            throw new RestApiException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 타입이 허용되는 이미지 타입인지 확인
        String contentType = file.getContentType();
        if (!allowedImageTypes.contains(contentType)) {
            throw new RestApiException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    // 원본 파일명을 기반으로 고유한 파일명을 생성하는 메서드
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        // 원본 파일명에서 확장자 추출
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        // UUID를 사용하여 고유한 파일명 생성
        return UUID.randomUUID().toString() + extension;
    }

    // 기존에 업로드된 이미지 파일을 삭제하는 메서드
    public void deleteImage(String imageUrl) {
        // URL이 유효하고 uploads 경로로 시작하는지 확인
        if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
            // URL에서 파일명 추출
            String fileName = imageUrl.substring("/uploads/".length());
            String filePath = uploadPath + fileName;

            try {
                // 파일이 존재하면 삭제
                Files.deleteIfExists(Paths.get(filePath));
                log.info("File deleted successfully: {}", fileName);
            } catch (IOException e) {
                log.error("Failed to delete file: {}", fileName, e);
            }
        }
    }
}