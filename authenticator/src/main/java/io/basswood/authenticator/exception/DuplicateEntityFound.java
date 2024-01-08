package io.basswood.authenticator.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEntityFound extends RootException {
    public DuplicateEntityFound(String type, String id) {
        super("An entity of type: " + type + " with id: " + id + " already exists",
                null, ErrorCode.duplicate_entity, HttpStatus.CONFLICT.value());
    }

    public DuplicateEntityFound(Class type, String id) {
        this(type.getName(), id);
    }
}
