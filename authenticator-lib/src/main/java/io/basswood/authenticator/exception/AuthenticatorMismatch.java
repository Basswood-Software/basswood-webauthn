package io.basswood.authenticator.exception;

import java.util.UUID;

public class AuthenticatorMismatch extends RootException {
    private static final String DEFAULT_MESSAGE = "This authenticator does not match the provided option";

    public AuthenticatorMismatch() {
        this(DEFAULT_MESSAGE, null);
    }

    public AuthenticatorMismatch(UUID aaguid) {
        this("The authenticator with id: " + aaguid.toString() + " does not support the requested option", null);
    }

    public AuthenticatorMismatch(String message, Throwable cause) {
        super(message, cause, ErrorCode.bad_request, 400);
    }
}
