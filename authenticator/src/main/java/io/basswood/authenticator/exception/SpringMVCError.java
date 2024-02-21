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

public class SpringMVCError extends AuthenticatorException {
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

    private static String mapMVCCode(Exception ex) {
        return switch (ex){
            case HttpRequestMethodNotSupportedException ignored -> "http_request_method_not_supported";
            case HttpMediaTypeNotSupportedException ignored -> "http_media_type_not_supported";
            case HttpMediaTypeNotAcceptableException ignored -> "http_media_type_not_acceptable";
            case MissingPathVariableException ignored -> "missing_path_variable";
            case MissingServletRequestParameterException ignored -> "missing_servlet_request_parameter";
            case ServletRequestBindingException ignored -> "servlet_request_binding";
            case ConversionNotSupportedException ignored -> "conversion_not_supported";
            case TypeMismatchException ignored -> "type_mismatch";
            case HttpMessageNotReadableException ignored -> "http_message_not_readable";
            case HttpMessageNotWritableException ignored -> "http_message_not_writable";
            case MethodArgumentNotValidException ignored -> "method_argument_not_valid";
            case MissingServletRequestPartException ignored -> "missing_servlet_request_part";
            case BindException ignored -> "bind_exception";
            case NoHandlerFoundException ignored -> "no_handler_found";
            case AsyncRequestTimeoutException ignored -> "async_request_timeout";
            default -> "server_error";
        };
    }

    public static boolean isSpringMVCException(Exception exception) {
        return mvcExceptions.contains(exception.getClass());
    }
}
