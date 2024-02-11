package io.basswood.authenticator.exception;

public class BadRequest extends RootException {
    private static final String DEFAULT_MESSAGE = "Bad Request";

    public BadRequest() {
        this(DEFAULT_MESSAGE);
    }

    public BadRequest(String message) {
        this(message, null);
    }

    public BadRequest(String message, Throwable cause) {
        super(message, cause, ErrorCode.bad_request, 400);
    }
}
