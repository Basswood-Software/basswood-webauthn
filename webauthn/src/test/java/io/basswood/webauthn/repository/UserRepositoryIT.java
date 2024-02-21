package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.model.user.Username;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserRepositoryIT extends BaseRepositoryIT {
    @Autowired
    private UserRepository userRepository;

    private User homerSimpson;

    @BeforeEach
    void setup() {
        homerSimpson = User.builder()
                .userHandle(UUID.randomUUID().toString())
                .displayName("Homer Simpson")
                .usernames(Set.of(Username.builder()
                        .username("homer.simpson@aol.com")
                        .build()))
                .build();
        homerSimpson.getUsernames().stream().forEach(username -> username.setUser(homerSimpson));
        User saved = userRepository.save(homerSimpson);
    }

    @Test
    void createUser() {
        Optional<User> distinctByUserHandle = userRepository.findDistinctByUserHandle(homerSimpson.getUserHandle());
        Assertions.assertTrue(distinctByUserHandle.isPresent());
        Assertions.assertEquals(homerSimpson.getDisplayName(), distinctByUserHandle.get().getDisplayName());
    }
}