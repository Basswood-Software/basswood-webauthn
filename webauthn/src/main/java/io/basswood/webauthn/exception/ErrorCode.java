package io.basswood.webauthn.exception;

public enum ErrorCode {
    server_error,
    bad_request,
    not_found,
    duplicate_entity,

    token_validation_error,
    // Spring MVC errors start
    http_request_method_not_supported,
    http_mediatype_not_supported,
    http_media_type_not_acceptable,
    missing_path_variable,
    servlet_request_binding,
    conversion_not_supported,
    type_mismatch,
    http_message_not_readable,
    http_message_not_writable,
    method_argument_not_valid,
    missing_servlet_request_part,
    bind_exception,
    no_handler_found,
    async_request_timeout,
    // Spring MVC errors end
}