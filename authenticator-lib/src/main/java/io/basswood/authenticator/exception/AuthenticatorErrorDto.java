package io.basswood.authenticator.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticatorErrorDto {
    private String errorCode;
    private String errorMessage;
    private int errorStatus;
    private long timestamp;
    private String path;
    private StackTraceElement[] trace;
}
