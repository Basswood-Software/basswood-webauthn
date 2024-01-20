package io.basswood.webauthn.exception;

import org.springframework.http.HttpStatus;

public class KeystoreException extends RootException {

    public KeystoreException(String message) {
        this(message, null);
    }

    public KeystoreException(String message, Throwable cause) {
        super(message, cause, ErrorCode.keystore_error, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
