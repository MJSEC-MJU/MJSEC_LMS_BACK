package com.mjsec.lms.service;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyActivity;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.ImageResponse;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.StudyActivityRepository;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.util.ValidationUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ImageService {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    private final StudyActivityRepository studyActivityRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ValidationUtils validationUtils;

    // 지원하는 이미지 파일 확장자와 MIME 타입 매핑
    private static final Map<String, MediaType> SUPPORTED_IMAGE_TYPES = new HashMap<>();

    static {
        SUPPORTED_IMAGE_TYPES.put("jpg", MediaType.IMAGE_JPEG);
        SUPPORTED_IMAGE_TYPES.put("jpeg", MediaType.IMAGE_JPEG);
        SUPPORTED_IMAGE_TYPES.put("png", MediaType.IMAGE_PNG);
        SUPPORTED_IMAGE_TYPES.put("gif", MediaType.IMAGE_GIF);
        SUPPORTED_IMAGE_TYPES.put("webp", MediaType.valueOf("image/webp"));
    }

    public ImageService(StudyActivityRepository studyActivityRepository,
                        GroupMemberRepository groupMemberRepository,
                        ValidationUtils validationUtils) {
        this.studyActivityRepository = studyActivityRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.validationUtils = validationUtils;
    }

    /**
     * 스터디 활동 이미지 조회 (권한 검증 포함)
     * 해당 스터디 그룹의 멤버만 이미지 접근 가능
     *
     * @param filename 이미지 파일명
     * @param currentUserStudentNumber 현재 사용자 학번
     * @return 이미지 응답 객체
     */
    @Transactional(readOnly = true)
    public ImageResponse getImage(String filename, Long currentUserStudentNumber) {
        log.info("Getting image: {} for user: {}", filename, currentUserStudentNumber);

        // 사용자 검증
        User currentUser = validationUtils.validateUser(currentUserStudentNumber);

        // 파일명으로 스터디 활동 찾기
        StudyActivity studyActivity = findStudyActivityByImageUrl(filename);

        if (studyActivity != null) {
            // 스터디 활동 이미지인 경우 - 해당 그룹 멤버만 접근 가능
            StudyGroup studyGroup = studyActivity.getStudyGroup();
            validationUtils.validateGroupMembership(currentUser, studyGroup);

            log.info("Access granted to study activity image for group: {} by user: {}",
                    studyGroup.getStudyId(), currentUserStudentNumber);
        } else {
            // 스터디 그룹 이미지인지 확인 (StudyGroup의 studyImage)
            if (!isStudyGroupImageAccessible(filename, currentUser)) {
                log.warn("Unauthorized image access attempt: {} by user: {}", filename, currentUserStudentNumber);
                throw new RestApiException(ErrorCode.UNAUTHORIZED_IMAGE_ACCESS);
            }
        }

        // 파일 시스템에서 이미지 로드
        return loadImageFromFileSystem(filename);
    }

    /**
     * 프로필 이미지 조회 (더 관대한 권한)
     * 인증된 사용자는 모든 프로필 이미지 접근 가능
     *
     * @param filename 프로필 이미지 파일명
     * @param currentUserStudentNumber 현재 사용자 학번
     * @return 이미지 응답 객체
     */
    @Transactional(readOnly = true)
    public ImageResponse getProfileImage(String filename, Long currentUserStudentNumber) {
        log.info("Getting profile image: {} for user: {}", filename, currentUserStudentNumber);

        // 기본 사용자 검증만 수행 (인증된 사용자면 모든 프로필 이미지 접근 가능)
        validationUtils.validateUser(currentUserStudentNumber);

        return loadImageFromFileSystem(filename);
    }

    /**
     * 파일명으로 스터디 활동 찾기
     */
    private StudyActivity findStudyActivityByImageUrl(String filename) {
        String imageUrl = "/uploads/" + filename;
        return studyActivityRepository.findAll()
                .stream()
                .filter(activity -> imageUrl.equals(activity.getImageUrl()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 스터디 그룹 이미지에 대한 접근 권한 확인
     * 해당 그룹의 멤버인지 확인
     */
    private boolean isStudyGroupImageAccessible(String filename, User currentUser) {
        String imageUrl = "/uploads/" + filename;

        // GroupMember 테이블에서 해당 사용자가 속한 그룹들 조회
        List<GroupMember> userGroups = groupMemberRepository.findByUserIdWithStudyGroup(currentUser.getUserId());

        // 그 중에서 해당 이미지 URL을 가진 스터디 그룹이 있는지 확인
        return userGroups.stream()
                .anyMatch(groupMember ->
                        imageUrl.equals(groupMember.getStudyGroup().getStudyImage()));
    }
    /**
     * 파일 시스템에서 실제 이미지 파일을 로드
     */
    private ImageResponse loadImageFromFileSystem(String filename) {
        try {
            // 보안: 경로 순회 공격 방지
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Potential path traversal attack detected: {}", filename);
                throw new RestApiException(ErrorCode.INVALID_FILE_NAME);
            }

            // 파일 경로 구성
            Path filePath = Paths.get(uploadPath).resolve(filename).normalize();

            // 업로드 디렉토리 밖으로 벗어나는지 확인
            if (!filePath.startsWith(Paths.get(uploadPath).normalize())) {
                log.warn("Path traversal attempt detected: {}", filename);
                throw new RestApiException(ErrorCode.INVALID_FILE_PATH);
            }

            // 파일 존재 여부 확인
            if (!Files.exists(filePath)) {
                log.error("Image file not found: {}", filename);
                throw new RestApiException(ErrorCode.IMAGE_NOT_FOUND);
            }

            // 파일 확장자로 MIME 타입 결정
            String extension = getFileExtension(filename).toLowerCase();
            MediaType mediaType = SUPPORTED_IMAGE_TYPES.get(extension);

            if (mediaType == null) {
                log.error("Unsupported image type: {}", extension);
                throw new RestApiException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
            }

            // 파일 리소스 생성
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists() || !resource.isReadable()) {
                log.error("Image file not readable: {}", filename);
                throw new RestApiException(ErrorCode.IMAGE_NOT_READABLE);
            }

            // 파일 크기 조회
            long contentLength = Files.size(filePath);

            log.info("Image loaded successfully: {}, size: {} bytes", filename, contentLength);

            return ImageResponse.builder()
                    .resource(resource)
                    .mediaType(mediaType)
                    .contentLength(contentLength)
                    .originalFilename(filename)
                    .build();

        } catch (IOException e) {
            log.error("Failed to load image: {}", filename, e);
            throw new RestApiException(ErrorCode.IMAGE_LOAD_FAILED);
        }
    }

    /**
     * 파일명에서 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

}