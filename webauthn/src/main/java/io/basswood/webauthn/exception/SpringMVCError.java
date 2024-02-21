package io.basswood.webauthn.exception;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;

public class SpringMVCError extends RootException {
    public static final String DEFAULT_MESSAGE = "Spring MVC error";

    private static final Set<Class<? extends Exception>> mvcExceptions = ImmutableSet.<Class<? extends Exception>>builder()
            .add(HttpRequestMethodNotSupportedException.class)
            .add(HttpMediaTypeNotSupportedException.class)
            .add(HttpMediaTypeNotAcceptableException.class)
            .add(MissingPathVariableException.class)
            .add(MissingServletRequestParameterException.class)
            .add(ServletRequestBindingException.class)
            .add(ConversionNotSupportedException.class)
            .add(TypeMismatchException.class)
            .add(HttpMessageNotReadableException.class)
            .add(HttpMessageNotWritableException.class)
            .add(MethodArgumentNotValidException.class)
            .add(MissingServletRequestPartException.class)
            .add(BindException.class)
            .add(NoHandlerFoundException.class)
            .add(AsyncRequestTimeoutException.class)
            .build();

    public SpringMVCError(Exception exception, int status) {
        super(DEFAULT_MESSAGE, exception, mapMVCCode(exception), status);
    }

    private static ErrorCode mapMVCCode(Exception ex) {
        if (HttpRequestMethodNotSupportedException.class == ex.getClass()) {
            return ErrorCode.http_request_method_not_supported;
        } else if (HttpMediaTypeNotSupportedException.class == ex.getClass()) {
            return ErrorCode.http_mediatype_not_supported;
        } else if (HttpMediaTypeNotAcceptableException.class == ex.getClass()) {
            return ErrorCode.http_media_type_not_acceptable;
        } else if (MissingPathVariableException.class == ex.getClass()) {
            return ErrorCode.missing_path_variable;
        } else if (MissingServletRequestParameterException.class == ex.getClass()) {
            return ErrorCode.missing_servlet_request_part;
        } else if (ServletRequestBindingException.class == ex.getClass()) {
            return ErrorCode.servlet_request_binding;
        } else if (ConversionNotSupportedException.class == ex.getClass()) {
            return ErrorCode.conversion_not_supported;
        } else if (TypeMismatchException.class == ex.getClass()) {
            return ErrorCode.type_mismatch;
        } else if (HttpMessageNotReadableException.class == ex.getClass()) {
            return ErrorCode.http_message_not_readable;
        } else if (HttpMessageNotWritableException.class == ex.getClass()) {
            return ErrorCode.http_message_not_writable;
        } else if (MethodArgumentNotValidException.class == ex.getClass()) {
            return ErrorCode.method_argument_not_valid;
        } else if (MissingServletRequestPartException.class == ex.getClass()) {
            return ErrorCode.missing_servlet_request_part;
        } else if (BindException.class == ex.getClass()) {
            return ErrorCode.bind_exception;
        } else if (NoHandlerFoundException.class == ex.getClass()) {
            return ErrorCode.no_handler_found;
        } else if (AsyncRequestTimeoutException.class == ex.getClass()) {
            return ErrorCode.async_request_timeout;
        } else {
            return ErrorCode.server_error;
        }
    }

    public static boolean isSpringMVCException(Exception exception) {
        return mvcExceptions.contains(exception.getClass());
    }
}
