package io.basswood.webauthn.security;

import io.basswood.webauthn.model.token.Role;

/**
 * @author shamualr
 * @since 1.0
 */
public class RoleMapping {
    public Role requireRole(String path){
        return switch (path){
            case String requestPath when requestPath.startsWith("/user") -> Role.user_manager;
            case String requestPath when requestPath.startsWith("/relying-party") -> Role.rp_manager;
            case String requestPath when requestPath.startsWith("/jwk") -> Role.jwk_manager;
            case String requestPath when requestPath.startsWith("/jwt") -> Role.token_manager;
            default -> Role.none;
        };
    }
}
