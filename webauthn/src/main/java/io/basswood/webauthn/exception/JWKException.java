package io.basswood.webauthn.exception;

import org.springframework.http.HttpStatus;

public class JWKException extends RootException {

    public JWKException(String message) {
        this(message, null);
    }

    public JWKException(String message, Throwable cause) {
        super(message, cause, ErrorCode.jwk_error, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
