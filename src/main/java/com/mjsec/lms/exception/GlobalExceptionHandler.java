package com.mjsec.lms.exception;

import static com.mjsec.lms.type.ErrorCode.BAD_REQUEST;
import static com.mjsec.lms.type.ErrorCode.INTERNAL_SERVER_ERROR;

import com.mjsec.lms.dto.ErrorResponse;
import com.mjsec.lms.dto.ErrorResponse.ValidationError;
import com.mjsec.lms.type.ErrorCode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * RestApiException(커스텀 에러) 처리
     *
     * @param e RestApiException
     * @return ResponseEntity
     */
    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<Object> handleCustomException(RestApiException e){

        ErrorCode errorCode = e.getErrorCode();
        log.error("RestApiException occurred : ErrorCode = {} message = {}",
                errorCode.name(), errorCode.getDescription());
        return handleExceptionInternal(errorCode);
    }
//
    /**
     * MethodArgumentNotValidException 처리 (Validation 실패 등)
     *
     * @param e MethodArgumentNotValidException
     * @return ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {

        log.error("Validation failed", e);
        return handleValidationException(e, BAD_REQUEST);
    }

    private ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException e, ErrorCode errorCode) {

        List<ValidationError> validationErrors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(ValidationError::of)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.name())
                .message(errorCode.getDescription())
                .errors(validationErrors)
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }

    /**
     * IllegalArgumentException 처리 (적절하지 않은 파라미터)
     *
     * @param e IllegalArgumentException
     * @return ResponseEntity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException e) {

        log.error("IllegalArgumentException occurred", e);
        return handleExceptionInternal(BAD_REQUEST);
    }

    /**
     * DataIntegrityViolationException 처리 (DB 제약 조건 위반)
     *
     * @param e DataIntegrityViolationException
     * @return ResponseEntity
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {

        log.error("DataIntegrityViolationException occurred", e);
        return handleExceptionInternal(BAD_REQUEST);
    }

    /**
     * Exception 처리
     *
     * @param e Exception
     * @return ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {

        log.error("Unexpected Exception occurred", e);
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(INTERNAL_SERVER_ERROR.getDescription());
    }

    private ResponseEntity<Object> handleExceptionInternal(ErrorCode errorCode) {

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponseBody(errorCode));
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorCode errorCode) {

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponseBody((MethodArgumentNotValidException) e, errorCode));
    }

    private ErrorResponse makeErrorResponseBody(ErrorCode errorCode) {

        return ErrorResponse.builder()
                .code(errorCode.name())
                .message(errorCode.getDescription())
                .build();
    }

    private ErrorResponse makeErrorResponseBody(BindException e, ErrorCode errorCode) {

        List<ValidationError> validationErrorList = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(ValidationError::of)
                .collect(Collectors.toList());

        return ErrorResponse.builder()
                .code(errorCode.name())
                .message(errorCode.getDescription())
                .errors(validationErrorList)
                .build();
    }
}
