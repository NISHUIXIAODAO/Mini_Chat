package com.easychat.hander;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------- 错误响应实体类 -------------------------
    @Data
    public static class ErrorResponse<T> {
        private long timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private T details;

        public ErrorResponse(int status, String error, String message, String path, T details) {
            this.timestamp = System.currentTimeMillis();
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.details = details;
        }
    }

    // ------------------------- 自定义异常基类 -------------------------
    public static class BusinessException extends RuntimeException {
        private final ErrorCode errorCode;
        private final String message;

        public BusinessException(ErrorCode errorCode) {
            super(errorCode.getMessage());
            this.errorCode = errorCode;
            this.message = errorCode.getMessage();
        }

        public BusinessException(ErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
            this.message = message;
        }

        public BusinessException(String message){
            super(message);
            this.message = message;
            this.errorCode = null;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    // ------------------------- 错误码枚举 -------------------------
    public enum ErrorCode {
        // 基础错误码
        SUCCESS(0, "成功"),
        BAD_REQUEST(400, "请求参数错误"),
        UNAUTHORIZED(401, "未授权"),
        FORBIDDEN(403, "禁止访问"),
        NOT_FOUND(404, "资源不存在"),
        INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

        // 业务错误码
        VALIDATION_FAILED(1001, "参数校验失败"),
        ACCOUNT_LOCKED(1002, "账户已被锁定"),
        INVALID_CREDENTIALS(1003, "用户名或密码错误"),
        USER_NOT_FOUND(1004, "用户不存在"),

        //my加
        JSON_ENTITY_FAILED(505,"convertJsonToObj异常"),
        JSON_TO_LIST_FAILED(506,"convertJsonArrayToList异常"),

        CODE_600(600,"chatMessage is null or not must to send "),
        CODE_BLACK(901,"对方已拉黑你"),
        CODE_UNEXIST(902,"你们还不是好友"),
        CODE_PARAM_ERROR(903, "获取聊天历史消息参数不足");


        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    // ------------------------- 异常处理器 -------------------------

    // 处理业务异常（默认返回400）
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse<Object> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.info("业务异常: [{}] {}", ex.getErrorCode().getCode(), ex.getMessage());
        return new ErrorResponse<>(
                ex.getErrorCode().getCode(),
                "Business Error",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // 处理登录相关认证异常（返回401）
    @ExceptionHandler({
            AccountLockedException.class,
            InvalidCredentialsException.class,
            UserNotFoundException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse<Object> handleAuthExceptions(BusinessException ex, HttpServletRequest request) {
        log.info("认证失败: {} - {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse<>(
                ex.getErrorCode().getCode(),
                "Authentication Error",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // 处理参数校验异常（@RequestBody）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse<Map<String, String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.info("参数校验失败: {}", errors);
        return buildValidationErrorResponse(request, errors);
    }

    // 处理参数校验异常（@RequestParam/@PathVariable）
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse<Map<String, String>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        violations.forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        log.info("参数校验失败: {}", errors);
        return buildValidationErrorResponse(request, errors);
    }

    // 处理权限不足异常（返回403）
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse<Object> handleAccessDeniedException(HttpServletRequest request) {
        log.info("拒绝访问: {}", request.getRequestURI());
        return new ErrorResponse<>(
                ErrorCode.FORBIDDEN.getCode(),
                "Access Denied",
                ErrorCode.FORBIDDEN.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // 处理其他未捕获异常（返回500）
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse<Object> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.info("系统异常: {}", ex.getMessage(), ex);
        return new ErrorResponse<>(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "Internal Server Error",
                "系统繁忙，请稍后再试",
                request.getRequestURI(),
                null
        );
    }

    // ------------------------- 自定义异常类 -------------------------

    // 账户锁定异常
    public static class AccountLockedException extends BusinessException {
        public AccountLockedException() {
            super(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    // 密码错误异常
    public static class InvalidCredentialsException extends BusinessException {
        public InvalidCredentialsException() {
            super(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // 用户不存在异常
    public static class UserNotFoundException extends BusinessException {
        public UserNotFoundException() {
            super(ErrorCode.USER_NOT_FOUND);
        }
    }

    // 资源不存在异常（通用）
    public static class ResourceNotFoundException extends BusinessException {
        public ResourceNotFoundException(String message) {
            super(ErrorCode.NOT_FOUND, message);
        }
    }

    // 数据冲突异常
    public static class ConflictException extends BusinessException {
        public ConflictException(String message) {
            super(ErrorCode.BAD_REQUEST, message);
        }
    }

    // ------------------------- 工具方法 -------------------------
    private ErrorResponse<Map<String, String>> buildValidationErrorResponse(
            HttpServletRequest request, Map<String, String> errors) {
        return new ErrorResponse<>(
                ErrorCode.VALIDATION_FAILED.getCode(),
                "Validation Error",
                ErrorCode.VALIDATION_FAILED.getMessage(),
                request.getRequestURI(),
                errors
        );
    }
}
