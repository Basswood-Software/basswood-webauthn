package io.basswood.authenticator.exception;

public class DuplicateEntityFound extends RootException {
    public DuplicateEntityFound(String type, String id) {
        super("An entity of type: " + type + " with id: " + id + " already exists",
                null, ErrorCode.duplicate_entity, 409);
    }

    public DuplicateEntityFound(Class type, String id) {
        this(type.getName(), id);
    }
}
