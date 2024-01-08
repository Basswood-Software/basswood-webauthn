package io.basswood.authenticator.exception;

import org.springframework.http.HttpStatus;

public class EntityNotFound extends RootException {
    public EntityNotFound(String type, String id) {
        super("Entity of type: " + type + " with id: " + id + " not found",
                null, ErrorCode.not_found, HttpStatus.NOT_FOUND.value());
    }

    public EntityNotFound(Class type, String id) {
        this(type.getName(), id);
    }
}
