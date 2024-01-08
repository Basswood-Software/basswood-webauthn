package io.basswood.authenticator.exception;

import org.springframework.http.HttpStatus;

public class SerializationException extends RootException {
    private static final String DEFAULT_MESSAGE = "Data Serialization error";
    public SerializationException() {
        this(DEFAULT_MESSAGE);
    }

    public SerializationException(String message) {
        this(message, null);
    }
    public SerializationException(Throwable cause) {
        this(DEFAULT_MESSAGE, cause);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.bad_request, HttpStatus.BAD_REQUEST.value());
    }
}
