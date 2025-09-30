package com.mjsec.lms.service;

import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.type.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@Slf4j
public class FileService {

    private static final int BUFFER_SIZE = 8192; // 8KB 청크 단위 처리
    private static final int MAX_HEADER_SIZE = 1024; // 헤더 검증용 최대 크기
    private static final double MAX_MEMORY_USAGE_RATIO = 0.8; // 최대 메모리 사용률 80%

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    // 파일 업로드 경로 설정값을 가져옴
    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    // Apache Tika 인스턴스
    private final Tika tika = new Tika();

    // 이미지 파일 시그니처 (Magic Numbers)
    private static final Map<String, byte[]> IMAGE_SIGNATURES = new HashMap<>();

    static {
        // JPEG
        IMAGE_SIGNATURES.put("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        // PNG
        IMAGE_SIGNATURES.put("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        // GIF
        IMAGE_SIGNATURES.put("gif87", "GIF87a".getBytes());
        IMAGE_SIGNATURES.put("gif89", "GIF89a".getBytes());
        // WebP
        IMAGE_SIGNATURES.put("webp_riff", "RIFF".getBytes());
        IMAGE_SIGNATURES.put("webp_sig", "WEBP".getBytes());
    }

    // 최대 파일 크기 설정 (기본값: 10MB)
    @Value("${file.upload.max-size:10485760}") // 10MB
    private long maxFileSize;

    private static final int MAX_IMAGE_COUNT = 5;

    // 허용되는 이미지 파일 타입 목록
    private final List<String> allowedImageTypes = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            "jsp", "jspx", "php", "php3", "php4", "php5", "phtml",
            "asp", "aspx", "ascx", "ashx", "asmx",
            "exe", "dll", "com", "bat", "cmd", "sh", "bash",
            "cgi", "pl", "py", "rb", "js", "jar", "war"
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

    // ========== 다중 이미지 처리 메서드들 ==========

    // 다중 이미지 업로드 메서드
    public List<String> uploadMultipleImages(List<MultipartFile> files) {

        return uploadMultipleImagesMemorySafe(files);
    }

    // 다중 이미지 업데이트
    public List<String> updateMultipleImages(List<String> currentImageUrls, List<MultipartFile> newImages,
                                             String entityName, Long entityId) {

        log.info("Updating multiple images for {}: {} - Current: {}, New: {}",
                entityName, entityId,
                currentImageUrls != null ? currentImageUrls.size() : 0,
                newImages != null ? newImages.size() : 0);

        //기존 이미지들 삭제
        if (currentImageUrls != null && !currentImageUrls.isEmpty()) {
            for (String imageUrl : currentImageUrls) {
                try {
                    deleteImage(imageUrl);
                    log.debug("Previous image deleted successfully: {}", imageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete previous image: {}, but continuing", imageUrl, e);
                }
            }
            log.info("Deleted {} existing images for {}: {}", currentImageUrls.size(), entityName, entityId);
        }

        //새 이미지가 제공된 경우 업로드
        if (newImages != null && !newImages.isEmpty()) {
            try {
                List<String> newImageUrls = uploadMultipleImages(newImages);
                log.info("New images uploaded successfully for {}: {} - {} images",
                        entityName, entityId, newImageUrls.size());
                return newImageUrls;
            } catch (Exception e) {
                log.error("Failed to upload new images for {}: {}", entityName, entityId, e);
                // 새 이미지 업로드 실패시 예외를 다시 던져서 트랜잭션 롤백 유도
                throw e;
            }
        }

        //새 이미지가 없으면 빈 리스트 반환 (기존 이미지들은 이미 삭제됨)
        log.info("No new images provided for {}: {}, all existing images deleted", entityName, entityId);
        return new ArrayList<>();
    }

    // 다중 이미지 삭제 메서드
    public void deleteMultipleImages(List<String> imageUrls) {

        if (imageUrls == null || imageUrls.isEmpty()) {
            log.debug("No images to delete");
            return;
        }

        int deletedCount = 0;
        for (String imageUrl : imageUrls) {
            try {
                deleteImage(imageUrl);
                deletedCount++;
            } catch (Exception e) {
                log.warn("Failed to delete image: {}, continuing with others", imageUrl, e);
            }
        }

        log.info("Deleted {}/{} images successfully", deletedCount, imageUrls.size());
    }

    // 다중 파일 업로드 시 메모리 사용량 관리
    public List<String> uploadMultipleImagesMemorySafe(List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            log.debug("No files provided for upload");
            return new ArrayList<>();
        }

        // 이미지 개수 제한 검증
        if (files.size() > MAX_IMAGE_COUNT) {
            log.warn("Too many images provided: {} (max: {})", files.size(), MAX_IMAGE_COUNT);
            throw new RestApiException(ErrorCode.TOO_MANY_IMAGES);
        }

        // 전체 파일 크기 계산
        long totalFileSize = files.stream().mapToLong(MultipartFile::getSize).sum();

        // 전체 파일에 대한 메모리 체크
        if (!checkMemoryAvailability(totalFileSize)) {
            log.error("Insufficient memory for multiple file upload. Total size: {} bytes", totalFileSize);
            throw new RestApiException(ErrorCode.INSUFFICIENT_MEMORY);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            // 빈 파일 스킵
            if (file.isEmpty()) {
                log.debug("Skipping empty file");
                continue;
            }

            String uploadedUrl = uploadImage(file);
            uploadedUrls.add(uploadedUrl);

            // 각 파일 업로드 후 메모리 상태 체크
            if (log.isDebugEnabled()) {
                long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
                long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
                double usageRatio = (double) usedMemory / maxMemory;
                log.debug("Memory usage after file upload: {:.2f}%", usageRatio * 100);
            }
        }

        log.info("Successfully uploaded {} images with total size: {} bytes",
                uploadedUrls.size(), totalFileSize);
        return uploadedUrls;
    }

    // 이미지 파일을 업로드하고 웹에서 접근 가능한 URL을 반환하는 메서드
    public String uploadImage(MultipartFile file) {

        // 파일 유효성 검사 수행
        validateImageFileMemorySafe(file);

        // 고유한 파일명 생성
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String filePath = uploadPath + fileName;

        try {
            Path targetLocation = Paths.get(filePath);

            // 스트림 기반 파일 복사 (메모리 효율적)
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("File uploaded successfully: {}", fileName);
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

    /*
    ------- PRIVATE ----------
     */

    // 파일 시그니처 검증 메서드
    private boolean verifyImageSignature(byte[] fileBytes, String expectedExtension) {

        if (fileBytes == null || fileBytes.length < 12) {
            return false;
        }

        expectedExtension = expectedExtension.toLowerCase();

        try {
            // JPEG 검증
            if (expectedExtension.equals("jpg") || expectedExtension.equals("jpeg")) {
                return fileBytes[0] == (byte) 0xFF &&
                        fileBytes[1] == (byte) 0xD8 &&
                        fileBytes[2] == (byte) 0xFF;
            }

            // PNG 검증
            if (expectedExtension.equals("png")) {
                byte[] pngSignature = IMAGE_SIGNATURES.get("png");
                for (int i = 0; i < pngSignature.length; i++) {
                    if (fileBytes[i] != pngSignature[i]) {
                        return false;
                    }
                }
                return true;
            }

            // GIF 검증
            if (expectedExtension.equals("gif")) {
                String header = new String(fileBytes, 0, 6);
                return header.equals("GIF87a") || header.equals("GIF89a");
            }

            // WebP 검증
            if (expectedExtension.equals("webp")) {
                String riff = new String(fileBytes, 0, 4);
                String webp = new String(fileBytes, 8, 4);
                return riff.equals("RIFF") && webp.equals("WEBP");
            }

            return false;
        } catch (Exception e) {
            log.error("Error verifying image signature", e);
            return false;
        }
    }

    // 원본 파일명을 기반으로 고유한 파일명을 생성하는 메서드
    private String generateUniqueFileName(String originalFileName) {

        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RestApiException(ErrorCode.INVALID_FILE_NAME);
        }

        String extension = extractAndValidateExtension(originalFileName);

        String safeFileName = UUID.randomUUID().toString() + "." + extension;

        if (safeFileName.contains("..") || safeFileName.contains("/") || safeFileName.contains("\\")) {
            log.error("Path traversal attempt in generated filename: {}", safeFileName);
            throw new RestApiException(ErrorCode.INVALID_FILE_NAME);
        }

        log.info("Safe filename generated: {} -> {}", originalFileName, safeFileName);
        return safeFileName;
    }

    private void validateImageFileMemorySafe(MultipartFile file) {

        // 기본 검증
        if (file.isEmpty()) {
            throw new RestApiException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > maxFileSize) {
            throw new RestApiException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 메모리 사용량 체크
        if (!checkMemoryAvailability(file.getSize())) {
            log.error("Insufficient memory for file upload. File size: {} bytes", file.getSize());
            throw new RestApiException(ErrorCode.INSUFFICIENT_MEMORY);
        }

        // 원본 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RestApiException(ErrorCode.INVALID_FILE_NAME);
        }

        // 확장자 추출 및 검증
        String extension = extractAndValidateExtension(originalFilename);

        // 스트림 기반 파일 검증
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {

            // 헤더만 읽어서 Magic Number 검증 (메모리 효율적)
            byte[] headerBytes = readHeader(inputStream, MAX_HEADER_SIZE);

            // Magic Number 검증
            if (!verifyImageSignature(headerBytes, extension)) {
                log.error("File signature mismatch for file: {}. Expected: {}",
                        originalFilename, extension);
                throw new RestApiException(ErrorCode.INVALID_FILE_TYPE);
            }

            // MIME 타입 검출 (스트림 기반)
            String detectedMimeType = detectMimeTypeFromStream(inputStream, originalFilename);

            // 검출된 MIME 타입 검증
            if (!allowedImageTypes.contains(detectedMimeType)) {
                log.error("Detected MIME type {} is not allowed. File: {}",
                        detectedMimeType, originalFilename);
                throw new RestApiException(ErrorCode.INVALID_FILE_TYPE);
            }

            // Content-Type과 실제 검출된 타입 비교
            validateContentType(file.getContentType(), detectedMimeType, originalFilename);

            log.info("File validation successful. File: {}, Type: {}, Size: {} bytes",
                    originalFilename, detectedMimeType, file.getSize());

        } catch (IOException e) {
            log.error("Failed to read file stream: {}", originalFilename, e);
            throw new RestApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 메모리 가용성 체크
    private boolean checkMemoryAvailability(long fileSize) {

        try {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();

            // 현재 메모리 사용률 계산
            double currentUsageRatio = (double) usedMemory / maxMemory;

            // 파일 처리 후 예상 메모리 사용률 계산
            double projectedUsageRatio = (double) (usedMemory + fileSize) / maxMemory;

            log.debug("Memory usage - Current: {:.2f}%, Projected: {:.2f}%, Max allowed: {:.2f}%",
                    currentUsageRatio * 100, projectedUsageRatio * 100, MAX_MEMORY_USAGE_RATIO * 100);

            return projectedUsageRatio <= MAX_MEMORY_USAGE_RATIO;

        } catch (Exception e) {
            log.warn("Failed to check memory availability, allowing upload", e);
            return true; // 메모리 체크 실패 시 업로드 허용 (안전장치)
        }
    }

    // 스트림에서 헤더만 읽기
    private byte[] readHeader(InputStream inputStream, int maxHeaderSize) throws IOException {

        if (!inputStream.markSupported()) {
            throw new IOException("Stream does not support mark/reset");
        }

        inputStream.mark(maxHeaderSize); // 스트림 위치 마킹

        byte[] headerBytes = new byte[maxHeaderSize];
        int bytesRead = inputStream.read(headerBytes);

        inputStream.reset(); // 스트림 위치 리셋

        if (bytesRead <= 0) {
            throw new IOException("Failed to read file header");
        }

        // 실제 읽은 크기만큼만 반환
        byte[] actualHeader = new byte[bytesRead];
        System.arraycopy(headerBytes, 0, actualHeader, 0, bytesRead);

        return actualHeader;
    }

    // 스트림 기반 MIME 타입 검출
    private String detectMimeTypeFromStream(InputStream inputStream, String filename) throws IOException {

        inputStream.mark(BUFFER_SIZE); // 스트림 위치 마킹

        try {
            return tika.detect(inputStream, filename);
        } finally {
            inputStream.reset(); // 스트림 위치 리셋
        }
    }

    // 확장자 추출 및 검증
    private String extractAndValidateExtension(String originalFileName) {

        // 파일명에서 모든 점(.) 위치 검사
        String[] parts = originalFileName.split("\\.");
        if (parts.length > 2) {
            log.warn("Double extension detected: {}", originalFileName);
            throw new RestApiException(ErrorCode.INVALID_FILE_NAME);
        }

        // 실제 확장자 추출
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
            extension = originalFileName.substring(lastDotIndex + 1).toLowerCase();
        }

        // 확장자가 없거나 빈 경우 거부
        if (extension.isEmpty()) {
            log.warn("No extension found in file: {}", originalFileName);
            throw new RestApiException(ErrorCode.INVALID_FILE_TYPE);
        }

        // 위험한 확장자 블랙리스트 검증
        for (String dangerous : DANGEROUS_EXTENSIONS) {
            if (originalFileName.toLowerCase().contains("." + dangerous)) {
                log.error("Dangerous extension detected: {}", originalFileName);
                throw new RestApiException(ErrorCode.INVALID_FILE_TYPE);
            }
        }

        // 허용된 확장자 화이트리스트 검증
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Not allowed extension: {}", extension);
            throw new RestApiException(ErrorCode.INVALID_FILE_TYPE);
        }

        return extension;
    }

    // Content-Type 검증
    private void validateContentType(String declaredContentType, String detectedMimeType, String filename) {

        if (declaredContentType != null && !declaredContentType.equals(detectedMimeType)) {
            log.warn("Content-Type mismatch. Declared: {}, Detected: {} for file: {}",
                    declaredContentType, detectedMimeType, filename);
            // 실제 타입이 허용된 이미지면 계속 진행
        }
    }
}