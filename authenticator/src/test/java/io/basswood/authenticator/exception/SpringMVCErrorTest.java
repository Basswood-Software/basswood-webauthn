package io.basswood.authenticator.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

class SpringMVCErrorTest {
    @Test
    void mapMVCCode(){
        Assertions.assertEquals("http_request_method_not_supported",
                new SpringMVCError(Mockito.mock(HttpRequestMethodNotSupportedException.class), 500).getErrorCode());
        Assertions.assertEquals("http_media_type_not_supported",
                new SpringMVCError(Mockito.mock(HttpMediaTypeNotSupportedException.class), 500).getErrorCode());
        Assertions.assertEquals("http_media_type_not_acceptable",
                new SpringMVCError(Mockito.mock(HttpMediaTypeNotAcceptableException.class), 500).getErrorCode());
        Assertions.assertEquals("missing_path_variable",
                new SpringMVCError(Mockito.mock(MissingPathVariableException.class), 500).getErrorCode());
        Assertions.assertEquals("missing_servlet_request_parameter",
                new SpringMVCError(Mockito.mock(MissingServletRequestParameterException.class), 500).getErrorCode());
        Assertions.assertEquals("servlet_request_binding",
                new SpringMVCError(Mockito.mock(ServletRequestBindingException.class), 500).getErrorCode());
        Assertions.assertEquals("conversion_not_supported",
                new SpringMVCError(Mockito.mock(ConversionNotSupportedException.class), 500).getErrorCode());
        Assertions.assertEquals("type_mismatch",
                new SpringMVCError(Mockito.mock(TypeMismatchException.class), 500).getErrorCode());
        Assertions.assertEquals("http_message_not_readable",
                new SpringMVCError(Mockito.mock(HttpMessageNotReadableException.class), 500).getErrorCode());
        Assertions.assertEquals("http_message_not_writable",
                new SpringMVCError(Mockito.mock(HttpMessageNotWritableException.class), 500).getErrorCode());
        Assertions.assertEquals("method_argument_not_valid",
                new SpringMVCError(Mockito.mock(MethodArgumentNotValidException.class), 500).getErrorCode());
        Assertions.assertEquals("missing_servlet_request_part",
                new SpringMVCError(Mockito.mock(MissingServletRequestPartException.class), 500).getErrorCode());
        Assertions.assertEquals("bind_exception",
                new SpringMVCError(Mockito.mock(BindException.class), 500).getErrorCode());
        Assertions.assertEquals("no_handler_found",
                new SpringMVCError(Mockito.mock(NoHandlerFoundException.class), 500).getErrorCode());
        Assertions.assertEquals("async_request_timeout",
                new SpringMVCError(Mockito.mock(AsyncRequestTimeoutException.class), 500).getErrorCode());
        Assertions.assertEquals("server_error",
                new SpringMVCError(Mockito.mock(Exception.class), 500).getErrorCode());
    }

    @Test
    void isSpringMVCException(){
        SpringMVCError.mvcExceptions.forEach(ex ->
            Assertions.assertTrue(SpringMVCError.isSpringMVCException(Mockito.mock(ex)))
        );
    }
}