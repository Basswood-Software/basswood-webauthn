package io.basswood.authenticator.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AlgorithmMismatch extends RootException {
    private static final String DEFAULT_MESSAGE = "This authenticator does not support the algorithms requested.";
    public AlgorithmMismatch() {
        this(DEFAULT_MESSAGE, null);
    }

    public AlgorithmMismatch(UUID aaguid) {
        this("The authenticator with id: "+aaguid.toString()+" does not support the requested algorithm", null);
    }

    public AlgorithmMismatch(String message, Throwable cause) {
        super(message, cause, ErrorCode.bad_request, HttpStatus.BAD_REQUEST.value());
    }
}
