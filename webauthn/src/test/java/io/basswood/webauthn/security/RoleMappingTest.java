package io.basswood.webauthn.security;

import io.basswood.webauthn.model.token.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RoleMappingTest {
    @Test
    void testMapping() {
        RoleMapping roleMapping = new RoleMapping();
        Assertions.assertEquals(Role.user_manager, roleMapping.requireRole("/user/1234"));
        Assertions.assertEquals(Role.rp_manager, roleMapping.requireRole("/relying-party/1234"));
        Assertions.assertEquals(Role.jwk_manager, roleMapping.requireRole("/jwk/1234"));
        Assertions.assertEquals(Role.none, roleMapping.requireRole("/webauthn/registration"));
    }
}