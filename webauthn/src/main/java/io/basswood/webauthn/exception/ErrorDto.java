package io.basswood.webauthn.exception;

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
public class ErrorDto {
    private String errorCode;
    private String errorMessage;
    private int status;
    private long timestamp;
    private String path;
    private StackTraceElement[] trace;
}
