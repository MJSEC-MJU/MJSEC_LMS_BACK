package com.mjsec.lms.util;

import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
@Slf4j
public class FileUtils {

    private static final int BUFFER_SIZE = 8192; // 8KB 청크 단위 처리

    // Apache Tika 인스턴스
    private static final Tika tika = new Tika();

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

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            "jsp", "jspx", "php", "php3", "php4", "php5", "phtml",
            "asp", "aspx", "ascx", "ashx", "asmx",
            "exe", "dll", "com", "bat", "cmd", "sh", "bash",
            "cgi", "pl", "py", "rb", "js", "jar", "war"
    );

    // 원본 파일명을 기반으로 고유한 파일명을 생성하는 메서드
    public static String generateUniqueFileName(String originalFileName) {

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

    // 파일 시그니처 검증 메서드
    public static boolean verifyImageSignature(byte[] fileBytes, String expectedExtension) {

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

    // 확장자 추출 및 검증
    public static String extractAndValidateExtension(String originalFileName) {

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
    public static void validateContentType(String declaredContentType, String detectedMimeType, String filename) {

        if (declaredContentType != null && !declaredContentType.equals(detectedMimeType)) {
            log.warn("Content-Type mismatch. Declared: {}, Detected: {} for file: {}",
                    declaredContentType, detectedMimeType, filename);
            // 실제 타입이 허용된 이미지면 계속 진행
        }
    }

    // 스트림에서 헤더만 읽기
    public static byte[] readHeader(InputStream inputStream, int maxHeaderSize) throws IOException {

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
    public static String detectMimeTypeFromStream(InputStream inputStream, String filename) throws IOException {

        inputStream.mark(BUFFER_SIZE); // 스트림 위치 마킹

        try {
            return tika.detect(inputStream, filename);
        } finally {
            inputStream.reset(); // 스트림 위치 리셋
        }
    }


}
