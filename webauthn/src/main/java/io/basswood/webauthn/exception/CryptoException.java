package io.basswood.webauthn.exception;

import org.springframework.http.HttpStatus;

public class CryptoException extends RootException {

    public CryptoException(String message) {
        this(message, null);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause, ErrorCode.crypto_error, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
