package com.mjsec.lms.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    ANNOUNCEMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "공지사항에 접근할 권한이 없습니다."),
    ANNOUNCEMENT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 생성에 실패했습니다."),
    ANNOUNCEMENT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 수정에 실패했습니다."),
    ANNOUNCEMENT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 삭제에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}