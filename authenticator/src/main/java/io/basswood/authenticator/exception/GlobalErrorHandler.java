package io.basswood.authenticator.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author shamualr
 * @since 1.0
 */
@Slf4j
@ControllerAdvice
public class GlobalErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<ErrorDto> handleUncaughtExceptions(Exception ex, WebRequest request) {
        log.error("Uncaught exception", ex);
        return handleRootException(new RootException(ex), request);
    }

    @ExceptionHandler({RootException.class})
    public ResponseEntity<ErrorDto> handleRootException(RootException ex, WebRequest request) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ex.toErrorDto(((ServletWebRequest) request).getRequest().getRequestURI()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (SpringMVCError.isSpringMVCException(ex)) {
            ResponseEntity<ErrorDto> errorDtoResponseEntity = handleRootException(new SpringMVCError(ex, status.value()), request);
            return ResponseEntity.status(errorDtoResponseEntity.getStatusCode())
                    .body(errorDtoResponseEntity.getBody());
        } else {
            return super.handleExceptionInternal(ex, body, headers, status, request);
        }
    }
}