package io.basswood.authenticator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class RootException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Unknown Server Error";
    private static final int DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR.value();
    private static final ErrorCode DEFAULT_ERROR_CODE = ErrorCode.server_error;

    @Getter
    private ErrorCode errorCode;
    @Getter
    private int httpStatus;

    public RootException() {
        this(DEFAULT_MESSAGE, DEFAULT_ERROR_CODE, DEFAULT_STATUS);
    }

    public RootException(ErrorCode errorCode, int httpStatus) {
        this(DEFAULT_MESSAGE, errorCode, httpStatus);
    }

    public RootException(String message, ErrorCode errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public RootException(String message) {
        this(message, DEFAULT_ERROR_CODE, DEFAULT_STATUS);
    }

    public RootException(String message, Throwable cause, ErrorCode errorCode, int httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public RootException(String message, Throwable cause) {
        this(message, cause, DEFAULT_ERROR_CODE, DEFAULT_STATUS);
    }

    public RootException(Throwable cause) {
        this(DEFAULT_MESSAGE, cause, DEFAULT_ERROR_CODE, DEFAULT_STATUS);
    }

    public ErrorDto toErrorDto(String path){
        return ErrorDto.builder()
                .errorCode(getErrorCode().name())
                .errorMessage(getMessage())
                .status(getHttpStatus())
                .timestamp(System.currentTimeMillis())
                .path(path)
                .trace(getStackTrace())
                .build();
    }
}
