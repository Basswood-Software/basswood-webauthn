package io.basswood.authenticator.exception;

import org.springframework.http.HttpStatus;

public class BadRequest extends RootException {
    private static final String DEFAULT_MESSAGE = "Bad Request";
    public BadRequest() {
        this(DEFAULT_MESSAGE);
    }

    public BadRequest(String message) {
        this(message, null);
    }

    public BadRequest(String message, Throwable cause) {
        super(message, cause, ErrorCode.bad_request, HttpStatus.BAD_REQUEST.value());
    }
}
