package io.basswood.authenticator.exception;

public class EntityNotFound extends RootException {
    public EntityNotFound(String type, String id) {
        super("Entity of type: " + type + " with id: " + id + " not found",
                null, ErrorCode.not_found, 404);
    }

    public EntityNotFound(Class type, String id) {
        this(type.getName(), id);
    }
}
