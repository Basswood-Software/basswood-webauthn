package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.model.user.Username;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UsernameRepositoryIT extends BaseRepositoryIT {
    @Autowired
    private UsernameRepository usernameRepository;
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
    void findDistinctByUsername() {
        Optional<Username> distinctByUsername = usernameRepository.findDistinctByUsername("homer.simpson@aol.com");
        Assertions.assertTrue(distinctByUsername.isPresent());
        Username actual = distinctByUsername.get();
        Assertions.assertEquals(homerSimpson.getDisplayName(), actual.getUser().getDisplayName());
        Assertions.assertEquals(homerSimpson.getUserHandle(), actual.getUser().getUserHandle());
        Assertions.assertEquals(homerSimpson.getUsernames().stream().findFirst().get().getUsername(), actual.getUsername());
    }

    @Test
    void findByUsernameIn() {
        List<Username> byUsernameIn = usernameRepository.findByUsernameIn(List.of("homer.simpson@aol.com"));
        Assertions.assertEquals(1, byUsernameIn.size());
        Username actual = byUsernameIn.get(0);
        Assertions.assertEquals(homerSimpson.getDisplayName(), actual.getUser().getDisplayName());
        Assertions.assertEquals(homerSimpson.getUserHandle(), actual.getUser().getUserHandle());
        Assertions.assertEquals(homerSimpson.getUsernames().stream().findFirst().get().getUsername(), actual.getUsername());
    }
}