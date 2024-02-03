package io.basswood.authenticator.exception;

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

     static final Set<Class<? extends Exception>> mvcExceptions = ImmutableSet.<Class<? extends Exception>>builder()
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
        return switch (ex){
            case HttpRequestMethodNotSupportedException ignored -> ErrorCode.http_request_method_not_supported;
            case HttpMediaTypeNotSupportedException ignored -> ErrorCode.http_mediatype_not_supported;
            case HttpMediaTypeNotAcceptableException ignore -> ErrorCode.http_media_type_not_acceptable;
            case MissingPathVariableException ignore -> ErrorCode.missing_path_variable;
            case MissingServletRequestParameterException ignore -> ErrorCode.missing_servlet_request_parameter;
            case ServletRequestBindingException ignore -> ErrorCode.servlet_request_binding;
            case ConversionNotSupportedException ignore -> ErrorCode.conversion_not_supported;
            case TypeMismatchException ignore -> ErrorCode.type_mismatch;
            case HttpMessageNotReadableException ignore -> ErrorCode.http_message_not_readable;
            case HttpMessageNotWritableException ignore -> ErrorCode.http_message_not_writable;
            case MethodArgumentNotValidException ignore -> ErrorCode.method_argument_not_valid;
            case MissingServletRequestPartException ignore -> ErrorCode.missing_servlet_request_part;
            case BindException ignore -> ErrorCode.bind_exception;
            case NoHandlerFoundException ignore -> ErrorCode.no_handler_found;
            case AsyncRequestTimeoutException ignore -> ErrorCode.async_request_timeout;
            default -> ErrorCode.server_error;
        };
    }

    public static boolean isSpringMVCException(Exception exception) {
        return mvcExceptions.contains(exception.getClass());
    }
}
