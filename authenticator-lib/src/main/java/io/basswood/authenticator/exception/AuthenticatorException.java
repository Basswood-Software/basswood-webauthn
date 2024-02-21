package io.basswood.authenticator.exception;

import lombok.Getter;

public class AuthenticatorException extends RuntimeException {
    public static final String ERROR_CODE_BAD_REQUEST= "bad_request";
    public static final String ERROR_CODE_NOT_FOUND= "not_found";
    public static final String ERROR_CODE_DUPLICATE_ENTITY= "duplicate_entity";
    public static final String DEFAULT_ERROR_CODE = "server_error";
    private static final String DEFAULT_MESSAGE = "Unknown Authenticator Error";
    private static final int DEFAULT_STATUS = 500;

    @Getter
    private String errorCode;
    @Getter
    private int errorStatus;

    public AuthenticatorException(String message, String errorCode) {
        this(message, errorCode, DEFAULT_STATUS);
    }

    public AuthenticatorException(String message) {
        this(message, DEFAULT_ERROR_CODE, DEFAULT_STATUS);
    }

    public AuthenticatorException(String message, String errorCode, int errorStatus) {
        this(message, null, errorCode, errorStatus);
    }

    public AuthenticatorException(Throwable cause) {
        this(DEFAULT_MESSAGE, cause, DEFAULT_ERROR_CODE, DEFAULT_STATUS);
    }

    public AuthenticatorException(String message, Throwable cause, String errorCode, int errorStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorStatus = errorStatus;
    }

    public AuthenticatorErrorDto toErrorDto(String path) {
        return AuthenticatorErrorDto.builder()
                .errorCode(getErrorCode())
                .errorMessage(getMessage())
                .errorStatus(getErrorStatus())
                .timestamp(System.currentTimeMillis())
                .path(path)
                .trace(getStackTrace())
                .build();
    }
}
