package io.basswood.authenticator.exception;

public class ConflictException extends RootException {
    private static final String DEFAULT_MESSAGE = "Duplicate entity found";

    public ConflictException() {
        this(DEFAULT_MESSAGE);
    }

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause, ErrorCode.duplicate_entity, 409);
    }
}
