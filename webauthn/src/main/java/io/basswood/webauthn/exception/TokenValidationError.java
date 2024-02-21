package io.basswood.webauthn.exception;

import org.springframework.http.HttpStatus;

public class TokenValidationError extends RootException {
    private static final String DEFAULT_MESSAGE = "Token validation failed";
    public TokenValidationError() {
        this(DEFAULT_MESSAGE);
    }

    public TokenValidationError(String message) {
        this(message, null);
    }

    public TokenValidationError(String message, Throwable cause) {
        super(message, cause, ErrorCode.token_validation_error, HttpStatus.UNAUTHORIZED.value());
    }
    public TokenValidationError(String message, Throwable cause, int httpStatus) {
        super(message, cause, ErrorCode.token_validation_error, httpStatus);
    }
}
