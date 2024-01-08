package io.basswood.authenticator.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CredentialMismatch extends RootException {
    private static final String DEFAULT_MESSAGE = "This authenticator does not contain the allowed credentials.";
    public CredentialMismatch() {
        this(DEFAULT_MESSAGE, null);
    }

    public CredentialMismatch(UUID aaguid) {
        this("The authenticator with id: "+aaguid.toString()+" does not contain the allowed credentials", null);
    }

    public CredentialMismatch(String message, Throwable cause) {
        super(message, cause, ErrorCode.bad_request, HttpStatus.BAD_REQUEST.value());
    }
}
